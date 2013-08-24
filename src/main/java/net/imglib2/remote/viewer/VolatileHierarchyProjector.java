/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.imglib2.remote.viewer;

import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.Projector;
import net.imglib2.display.Volatile;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.ui.AbstractInterruptibleProjector;
import net.imglib2.view.Views;

/**
 * {@link Projector} for a hierarchy of {@link Volatile} inputs.  After each
 * {@link #map()} call, the projector has a {@link #isValid() state} that
 * signalizes whether all projected pixels were valid.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class VolatileHierarchyProjector< T, A extends Volatile< T >, B extends NumericType< B > > extends AbstractInterruptibleProjector< A, B >
{
	final protected ArrayList< RandomAccessible< A > > sources = new ArrayList< RandomAccessible< A > >();
	final protected ArrayImg< IntType, IntArray > mask;
	protected boolean valid = false;
	int s = 0;
	
	final protected FinalInterval sourceInterval;

	final long width;
	final long height;
	final long cr;
	
	final IterableInterval< B > iterableTarget;
	
	public VolatileHierarchyProjector(
			final List< ? extends RandomAccessible< A > > sources,
			final Converter< ? super A, B > converter,
			final RandomAccessibleInterval< B > target,
			final int numThreads )
	{
		super( Math.max( 2, sources.get( 0 ).numDimensions() ), converter, target, numThreads );

		this.sources.addAll( sources );
		s = sources.size();
	
		mask = ArrayImgs.ints( target.dimension( 0 ), target.dimension( 1 ) );
		iterableTarget = Views.iterable( target );
		
		for ( int d = 2; d < min.length; ++d )
			min[ d ] = max[ d ] = 0;

		max[ 0 ] = target.max( 0 );
		max[ 1 ] = target.max( 1 );
		sourceInterval = new FinalInterval( min, max );

		width = target.dimension( 0 );
		height = target.dimension( 1 );
		cr = -width;
		
		clearMask();
	}
	
	public void setSources( final List< RandomAccessible< A > > sources )
	{
		synchronized ( this.sources )
		{
			this.sources.addAll( sources );
			s = sources.size();
		}
	}
	
	/**
	 * @return true if all mapped pixels were {@link Volatile#isValid() valid}.
	 */
	public boolean isValid()
	{
		return valid;
	}
	
	/**
	 * Set all pixels in target to 100% transparent zero, and mask to all
	 * Integer.MAX_VALUE.
	 */
	public void clear()
	{
		final Cursor< B > targetCursor = iterableTarget.cursor();
		final ArrayCursor< IntType > maskCursor = mask.cursor();
		
		/* Despite the extra comparison, is consistently 60% faster than
		 * while ( targetCursor.hasNext() )
		 * {
		 *	targetCursor.next().set( 0x00000000 );
		 *	maskCursor.next().set( Integer.MAX_VALUE );
		 * }
		 * because it exploits CPU caching better.
		 */
		while ( targetCursor.hasNext() )
			targetCursor.next().setZero();
		while ( maskCursor.hasNext() )
			maskCursor.next().set( Integer.MAX_VALUE );
		
		s = sources.size();
	}
	
	/**
	 * Set all pixels in target to 100% transparent zero, and mask to all
	 * Integer.MAX_VALUE.
	 */
	public void clearMask()
	{
		final ArrayCursor< IntType > maskCursor = mask.cursor();
		
		while ( maskCursor.hasNext() )
			maskCursor.next().set( Integer.MAX_VALUE );
		
		s = sources.size();
	}
	
	@Override
	public boolean map()
	{
		System.out.println( "Mapping " + s + " levels." );
		
		final RandomAccess< B > targetRandomAccess = target.randomAccess( target );
		final ArrayRandomAccess< IntType > maskRandomAccess = mask.randomAccess( target );
		
		int i;
		
		valid = false;
		
		synchronized ( this.sources )
		{
			for ( i = 0; i < s && !valid; ++i )
			{
				valid = true;
				
				final RandomAccess< A > sourceRandomAccess = sources.get( i ).randomAccess( sourceInterval );
				sourceRandomAccess.setPosition( min );
				targetRandomAccess.setPosition( min[ 0 ], 0 );
				targetRandomAccess.setPosition( min[ 1 ], 1 );
				maskRandomAccess.setPosition( min[ 0 ], 0 );
				maskRandomAccess.setPosition( min[ 1 ], 1 );
			
				for ( long y = 0; y < height; ++y )
				{
					for ( long x = 0; x < width; ++x )
					{
						final IntType m = maskRandomAccess.get();
						if ( m.get() > i )
						{
							final A a = sourceRandomAccess.get();
							final boolean v = a.isValid();
							if ( v )
							{
								converter.convert( a, targetRandomAccess.get() );
								m.set( i );
							}
							valid &= v;
						}
						sourceRandomAccess.fwd( 0 );
						targetRandomAccess.fwd( 0 );
						maskRandomAccess.fwd( 0 );
					}
					sourceRandomAccess.move( cr, 0 );
					targetRandomAccess.move( cr, 0 );
					maskRandomAccess.move( cr, 0 );
					sourceRandomAccess.fwd( 1 );
					targetRandomAccess.fwd( 1 );
					maskRandomAccess.fwd( 1 );
				}
			}
			if ( valid )
				s = i - 1;
			valid = s == 1;
		}
		return true;
	}
	
	
	final public void draw()
	{
		final ImagePlus imp = new ImagePlus( "test", new ColorProcessor( ( int )target.dimension( 0 ), ( int )target.dimension( 1 ) ) );
		imp.show();
		
		long t, nTrials = 0;
		while ( s > 0 )
		{
			t = System.currentTimeMillis();
			map();
			System.out.println( "trial " + ( ++nTrials ) + ": s = " + s + " took " + ( System.currentTimeMillis() - t ) + "ms" );
			
			imp.setImage( ( ( ARGBScreenImage )target ).image() );
		}
	}
}
