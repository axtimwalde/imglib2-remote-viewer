/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package net.imglib2.remote.catmaid;

import net.imglib2.AbstractLocalizable;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.remote.AbstractRemoteRandomAccessibleInterval;
import net.imglib2.remote.Cache;
import net.imglib2.type.numeric.NumericType;

/**
 * A read-only {@link RandomAccessibleInterval} of ARGBTypes that generates its
 * pixel values from a CATMAID remote data set.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
abstract public class AbstractCATMAIDRandomAccessibleInterval<
		T extends NumericType< T >,
		E extends Cache.Entry< AbstractCATMAIDRandomAccessibleInterval< T, E >.Key, E > >
	extends AbstractRemoteRandomAccessibleInterval< T, AbstractCATMAIDRandomAccessibleInterval< T, E >.Key, E >
{
	public class Key
	{
		final protected long r, c, z;
		
		public Key( final long r, final long c, final long z )
		{
			this.r = r;
			this.c = c;
			this.z = z;
		}
		
		@Override
		public boolean equals( final Object other )
		{
			if ( this == other )
				return true;
			if ( !( other instanceof AbstractCATMAIDRandomAccessibleInterval.Key ) )
				return false;
			
			@SuppressWarnings( "unchecked" )
			final Key that = ( Key )other;
			return
					( this.r == that.r ) &&
					( this.c == that.c ) &&
					( this.z == that.z );
		}
		
		/**
		 * Return a hash code for the long tile index according to
		 * {@link Long#hashCode()}.  The hash has no collisions if the tile
		 * index is smaller than 2<sup>32</sup>.
		 */
		@Override
		public int hashCode() {
			final long value = ( z * rows + r ) * cols + c;
			return ( int )( value ^ ( value >>> 32 ) );
		}
	}
	
	abstract public class AbstractCATMAIDRandomAccess extends AbstractLocalizable implements RandomAccess< T >
	{
		protected long r, c;
		protected int xMod, yMod;
		final T t;

		public AbstractCATMAIDRandomAccess( final T t )
		{
			super( 3 );
			this.t = t;
			fetchPixels();
		}
		
		public AbstractCATMAIDRandomAccess( final AbstractCATMAIDRandomAccess template )
		{
			super( 3 );
			
			t = template.t.copy();
			
			position[ 0 ] = template.position[ 0 ];
			position[ 1 ] = template.position[ 1 ];
			position[ 2 ] = template.position[ 2 ];
			
			r = template.r;
			c = template.c;
			
			xMod = template.xMod;
			yMod = template.yMod;
		}
		
		abstract protected void fetchPixels();
		
		@Override
		public void fwd( final int d )
		{
			++position[ d ];
			switch ( d )
			{
			case 0:
				++xMod;
				if ( xMod == tileWidth )
				{
					++c;
					xMod = 0;
					fetchPixels();
				}
				break;
			case 1:
				++yMod;
				if ( yMod == tileHeight )
				{
					++r;
					yMod = 0;
					fetchPixels();
				}
				break;
			default:
				fetchPixels();
			}
		}

		@Override
		public void bck( final int d )
		{
			--position[ d ];
			switch ( d )
			{
			case 0:
				--xMod;
				if ( xMod == -1 )
				{
					--c;
					xMod = tileWidth - 1;
					fetchPixels();
				}
				break;
			case 1:
				--yMod;
				if ( yMod == -1 )
				{
					--r;
					yMod = tileHeight - 1;
					fetchPixels();
				}
				break;
			default:
				fetchPixels();
			}
		}

		@Override
		public void move( final int distance, final int d )
		{
			move( ( long )distance, d );
		}

		@Override
		public void move( final long distance, final int d )
		{
			position[ d ] += distance;
			switch ( d )
			{
			case 0:
				final long c1 = position[ 0 ] / tileWidth;
				if ( c1 == c )
					xMod -= distance;
				else
				{
					c = c1;
					xMod = ( int )( position[ 0 ] - c1 * tileWidth );
					fetchPixels();
				}
				break;
			case 1:
				final long r1 = position[ 1 ] / tileHeight;
				if ( r1 == r )
					yMod -= distance;
				else
				{
					r = r1;
					yMod = ( int )( position[ 1 ] - r1 * tileHeight );
					fetchPixels();
				}
				break;
			default:
				fetchPixels();
			}
		}

		@Override
		public void move( final Localizable localizable )
		{
			boolean updatePixels = false;
			
			final long dx = localizable.getLongPosition( 0 );
			final long dy = localizable.getLongPosition( 1 );
			
			position[ 0 ] += dx;
			position[ 1 ] += dy;
			
			final long c1 = position[ 0 ] / tileWidth;
			if ( c1 == c )
				xMod += dx;
			else
			{
				c = c1;
				xMod = ( int )( position[ 0 ] - c1 * tileWidth );
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			if ( r1 == r )
				yMod += dy;
			else
			{
				r = r1;
				yMod = ( int )( position[ 1 ] - r1 * tileHeight );
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				final long distance = localizable.getLongPosition( d );
				updatePixels |= distance != 0;
				position[ d ] += distance;
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void move( final int[] distance )
		{
			boolean updatePixels = false;
			
			position[ 0 ] += distance[ 0 ];
			position[ 1 ] += distance[ 1 ];
			
			final long c1 = position[ 0 ] / tileWidth;
			if ( c1 == c )
				xMod += distance[ 0 ];
			else
			{
				c = c1;
				xMod = ( int )( position[ 0 ] - c1 * tileWidth );
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			if ( r1 == r )
				yMod += distance[ 1 ];
			else
			{
				r = r1;
				yMod = ( int )( position[ 1 ] - r1 * tileHeight );
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				updatePixels |= distance[ d ] != 0;
				position[ d ] += distance[ d ];
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void move( final long[] distance )
		{
			boolean updatePixels = false;
			
			position[ 0 ] += distance[ 0 ];
			position[ 1 ] += distance[ 1 ];
			
			final long c1 = position[ 0 ] / tileWidth;
			if ( c1 == c )
				xMod += distance[ 0 ];
			else
			{
				c = c1;
				xMod = ( int )( position[ 0 ] - c1 * tileWidth );
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			if ( r1 == r )
				yMod += distance[ 1 ];
			else
			{
				r = r1;
				yMod = ( int )( position[ 1 ] - r1 * tileHeight );
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				updatePixels |= distance[ d ] != 0;
				position[ d ] += distance[ d ];
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void setPosition( final Localizable localizable )
		{
			boolean updatePixels = false;
			
			position[ 0 ] = localizable.getLongPosition( 0 );
			position[ 1 ] = localizable.getLongPosition( 1 );
			
			final long c1 = position[ 0 ] / tileWidth;
			xMod = ( int )( position[ 0 ] - c1 * tileWidth );
			if ( c1 != c )
			{
				c = c1;
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			yMod = ( int )( position[ 1 ] - r1 * tileHeight );
			if ( r1 != r )
			{
				r = r1;
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				final long p = localizable.getLongPosition( d );
				updatePixels |= position[ d ] != p;
				position[ d ] = p;
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void setPosition( final int[] pos )
		{
			boolean updatePixels = false;
			
			position[ 0 ] = pos[ 0 ];
			position[ 1 ] = pos[ 1 ];
			
			final long c1 = position[ 0 ] / tileWidth;
			xMod = ( int )( position[ 0 ] - c1 * tileWidth );
			if ( c1 != c )
			{
				c = c1;
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			yMod = ( int )( position[ 1 ] - r1 * tileHeight );
			if ( r1 != r )
			{
				r = r1;
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				updatePixels |= position[ d ] != pos[ d ];
				position[ d ] = pos[ d ];
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void setPosition( final long[] pos )
		{
			boolean updatePixels = false;
			
			position[ 0 ] = pos[ 0 ];
			position[ 1 ] = pos[ 1 ];
			
			final long c1 = position[ 0 ] / tileWidth;
			xMod = ( int )( position[ 0 ] - c1 * tileWidth );
			if ( c1 != c )
			{
				c = c1;
				updatePixels = true;
			}
			
			final long r1 = position[ 1 ] / tileHeight;
			yMod = ( int )( position[ 1 ] - r1 * tileHeight );
			if ( r1 != r )
			{
				r = r1;
				updatePixels = true;
			}
			
			for ( int d = 2; d < numDimensions(); ++d )
			{
				updatePixels |= position[ d ] != pos[ d ];
				position[ d ] = pos[ d ];
			}
			
			if ( updatePixels )
				fetchPixels();
		}

		@Override
		public void setPosition( final int pos, final int d )
		{
			switch ( d )
			{
			case 0:
				final long c1 = position[ 0 ] / tileWidth;
				xMod = ( int )( position[ 0 ] - c1 * tileWidth );
				position[ d ] = pos;
				if ( c1 != c )
				{
					c = c1;
					fetchPixels();
				}
				break;
			case 1:
				final long r1 = position[ 1 ] / tileHeight;
				yMod = ( int )( position[ 1 ] - r1 * tileHeight );
				position[ d ] = pos;
				if ( r1 != r )
				{
					r = r1;
					fetchPixels();
				}
				break;
			default:
				if ( position[ d ] != pos )
				{
					position[ d ] = pos;
					fetchPixels();
				}
				else
					position[ d ] = pos;
			}
		}

		@Override
		public void setPosition( final long pos, final int d )
		{
			switch ( d )
			{
			case 0:
				final long c1 = pos / tileWidth;
				xMod = ( int )( pos - c1 * tileWidth );
				position[ d ] = pos;
				if ( c1 != c )
				{
					c = c1;
					fetchPixels();
				}
				break;
			case 1:
				final long r1 = pos / tileHeight;
				yMod = ( int )( pos - r1 * tileHeight );
				position[ d ] = pos;
				if ( r1 != r )
				{
					r = r1;
					fetchPixels();
				}
				break;
			default:
				if ( position[ d ] != pos )
				{
					position[ d ] = pos;
					fetchPixels();
				}
				else
					position[ d ] = pos;
			}
		}
	}
	
	final protected String baseUrl;
	final protected long rows, cols, s;
	final protected int tileWidth, tileHeight;
	
	static protected long[] scaleDimensions(
			final long width,
			final long height,
			final long depth,
			final long s )
	{
		final double scale = 1.0 / Math.pow( 2, s );
		
		final long[] scaledDimensions = new long[]{
				( long )( width * scale ),
				( long )( height * scale ),
				( long )( depth * scale )
		};
		
		return scaledDimensions;
	}
	
	public AbstractCATMAIDRandomAccessibleInterval(
			final String url,
			final long width,
			final long height,
			final long depth,
			final long s,
			final int tileWidth,
			final int tileHeight )
	{
		super( scaleDimensions( width, height, depth, s ) );
		this.baseUrl = url;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.s = s;
		final double scale = 1.0 / Math.pow( 2, s );
		cols = ( long )Math.ceil( scale * width / tileWidth );
		rows = ( long )Math.ceil( scale * height / tileHeight );
		max[ 0 ] = ( long )( width * scale ) - 1;
		max[ 1 ] = ( long )( height * scale ) - 1;
		max[ 2 ] = depth - 1;
	}
	
	@Override
	public int numDimensions()
	{
		return 3;
	}

	protected E fetchPixels( final long r, final long c, final long z )
	{
		try
		{
			return fetchPixels2( r, c, z );
		}
		catch ( final OutOfMemoryError e )
		{
			System.gc();
			return fetchPixels2( r, c, z );
		}
	}
	
	abstract protected E fetchPixels2( final long r, final long c, final long z );
}
