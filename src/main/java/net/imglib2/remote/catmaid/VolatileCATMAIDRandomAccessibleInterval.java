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
package net.imglib2.remote.catmaid;



import ij.ImagePlus;
import ij.process.ColorProcessor;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import net.imglib2.Interval;
import net.imglib2.converter.Converter;
import net.imglib2.display.projector.IterableIntervalProjector2D;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.remote.Cache;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.volatiles.VolatileNumericType;
import net.imglib2.view.Views;

/**
 * <p>Read pixels served by the
 * <a href="http://hssl.cs.jhu.edu/wiki/doku.php?id=randal:hssl:research:brain:data_set_description">Open
 * Connectome Volume Cutout Service</a>.</p>
 * 
 * <p>The {@link VolatileCATMAIDRandomAccessibleInterval} is created with a base
 * URL, e.g.
 * <a href="http://openconnecto.me/emca/kasthuri11">http://openconnecto.me/emca/kasthuri11</a>
 * the interval dimensions, the dimensions of image cubes to be fetched and
 * cached, and an offset in <em>z</em>.  This offset constitutes the
 * 0-coordinate in <em>z</em> and should point to the first slice of the
 * dataset.</p> 
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class VolatileCATMAIDRandomAccessibleInterval extends
		AbstractCATMAIDRandomAccessibleInterval< VolatileNumericType< ARGBType >, VolatileCATMAIDRandomAccessibleInterval.Entry >
{
	public class Entry extends Cache.Entry<
			AbstractCATMAIDRandomAccessibleInterval< VolatileNumericType< ARGBType >, Entry >.Key,
			Entry >
	{
		public boolean valid;
		final public int[] data;
		
		public Entry( final Key key, final int[] data, final boolean valid )
		{
			super( key, cache );
			this.data = data;
			this.valid = valid;
		}
		
		public boolean isValid() { return valid; }
		public void setValid( final boolean valid ) { this.valid = valid; }
	}
	
	protected class Fetcher extends Thread
	{
		@Override
		final public void run()
		{
			while ( !isInterrupted() )
			{
//				System.out.println( "Queue size: " + queue.size() );
				Reference< Entry > ref;
				synchronized ( cache )
				{
					try { ref = queue.pop(); }
					catch ( final NoSuchElementException e ) { ref = null; }
				}
				if ( ref == null )
				{
					synchronized ( this )
					{
						try { wait(); }
						catch ( final InterruptedException e )
						{
							break;
						}
					}
				}
				else
				{
					final Entry entry;
					synchronized ( cache )
					{
						entry = ref.get();
						if ( entry != null )
						{
							/* replace WeakReferences by SoftReferences which promotes cache entries from third to second class citizens */
							synchronized ( cache )
							{
								entry.setValid( true );
								cache.remove( entry.key );
								cache.putSoft( entry.key, entry );
							}
						}
					}
					
					if ( entry != null )
					{	
						final String urlString =
								new
									StringBuffer( baseUrl ).
									append( entry.key.z ).
									append( "/" ).
									append( entry.key.r ).
									append( "_" ).
									append( entry.key.c ).
									append( "_" ).
									append( s ).
									append( ".jpg" ).
									toString();
						try
						{
							final URL url = new URL( urlString );
//							final Image image = toolkit.createImage( url );
						    final BufferedImage jpg = ImageIO.read( url );
						    
							/* This gymnastic is necessary to get reproducible gray
							 * values, just opening a JPG or PNG, even when saved by
							 * ImageIO, and grabbing its pixels results in gray values
							 * with a non-matching gamma transfer function, I cannot tell
							 * why... */
						    final BufferedImage image = new BufferedImage( tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB );
							image.createGraphics().drawImage( jpg, 0, 0, null );
							final PixelGrabber pg = new PixelGrabber( image, 0, 0, tileWidth, tileHeight, entry.data, 0, tileWidth );
							pg.grabPixels();
							
//							System.out.println( "success loading r=" + entry.key.r + " c=" + entry.key.c + " url(" + urlString + ")" );
							
						}
						catch (final IOException e)
						{
							System.out.println( "failed loading r=" + entry.key.r + " c=" + entry.key.c + " url(" + urlString + ")" );
						}
						catch (final InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
			synchronized ( cache )
			{
				queue.clear();
			}
		}
	}
	
	public class VolatileCATMAIDRandomAccess extends AbstractCATMAIDRandomAccess
	{
		protected Entry entry;
		
		public VolatileCATMAIDRandomAccess()
		{
			super( new VolatileNumericType< ARGBType >( new ARGBType() ) );
		}
		
		public VolatileCATMAIDRandomAccess( final VolatileCATMAIDRandomAccess template )
		{
			super( template );
		}
		
		@Override
		public VolatileNumericType< ARGBType > get()
		{
			t.get().set( entry.data[ tileWidth * yMod + xMod ] );
			t.setValid( entry.valid );
			return t;
		}

		@Override
		public VolatileCATMAIDRandomAccess copy()
		{
			return new VolatileCATMAIDRandomAccess( this );
		}

		@Override
		public VolatileCATMAIDRandomAccess copyRandomAccess()
		{
			return copy();
		}
		
		@Override
		protected void fetchPixels()
		{
			entry = VolatileCATMAIDRandomAccessibleInterval.this.fetchPixels( r, c, position[ 2 ] );
		}
	}
	
	final protected Fetcher fetcher;
	final protected LinkedList< Reference< Entry > > queue = new LinkedList< Reference< Entry > >();
	
	public VolatileCATMAIDRandomAccessibleInterval(
			final String url,
			final long width,
			final long height,
			final long depth,
			final int level,
			final int tileWidth,
			final int tileHeight )
	{
		super( url, width, height, depth, level, tileWidth, tileHeight );
		
		fetcher = new Fetcher();
		fetcher.start();
	}
	
	public VolatileCATMAIDRandomAccessibleInterval(
			final String url,
			final long width,
			final long height,
			final long depth,
			final int level )
	{
		this( url, width, height, depth, level, 256, 256 );
	}
	
	@Override
	public int numDimensions()
	{
		return 3;
	}
	
	
	@Override
	public VolatileCATMAIDRandomAccess randomAccess()
	{
		return new VolatileCATMAIDRandomAccess();
	}
	
	@Override
	public VolatileCATMAIDRandomAccess randomAccess( final Interval interval )
	{
		return randomAccess();
	}
		
	@Override
	protected Entry fetchPixels2( final long r, final long c, final long z )
	{
		final Reference< Entry > ref;
		final Key key;
		synchronized ( cache )
		{
			key = new Key( r, c, z );
			final Entry cachedEntry = cache.get( key );
			if ( cachedEntry != null )
				return cachedEntry;
			
			final int[] data = new int[ tileWidth * tileHeight ];
			ref = new WeakReference< Entry >( new Entry( key, data, false ) );
			//ref = new SoftReference< Entry >( new Entry( key, bytes, false ) );
			cache.putReference( key, ref );
			queue.push( ref );
		}
		synchronized ( fetcher )
		{
			fetcher.notify();
		}
		
		final Entry entry = ref.get();
		if ( entry != null )
			return entry;
		else
			return new Entry( key, new int[ tileWidth * tileHeight ], false );
	}
	
	@Override
	public void finalize()
	{
		fetcher.interrupt();
	}
	
	/**
	 * Test by displaying.
	 * 
	 * @param width
	 * @param height
	 */
	final public void draw( final int width, final int height )
	{
		final ARGBScreenImage target = new ARGBScreenImage( width, height );
		final IterableIntervalProjector2D< VolatileNumericType< ARGBType >, ARGBType > projector =
				new IterableIntervalProjector2D< VolatileNumericType<ARGBType>, ARGBType >(
						0,
						1,
						Views.offset( this, this.dimension( 0 ) / 2, this.dimension( 1 ) / 2, this.dimension( 2 ) / 2 ),
						target,
						new Converter< VolatileNumericType< ARGBType >, ARGBType >()
						{
							@Override
							public void convert( final VolatileNumericType< ARGBType > input, final ARGBType output )
							{
								if ( input.isValid() )
									output.set( input.get() );
							}
						} );
		
		final ImagePlus imp = new ImagePlus( "test", new ColorProcessor( ( int )target.dimension( 0 ), ( int )target.dimension( 1 ) ) );
		imp.show();
		
		long t, nTrials = 0;
		
		while ( true )
		{
			t = System.currentTimeMillis();
			projector.map();
			System.out.println( "trial " + ( ++nTrials ) + ": s = " + s + " took " + ( System.currentTimeMillis() - t ) + "ms" );
				
			imp.setImage( target.image() );
		}
	}
	
	final static public void main( final String... args )
	{
		final long width = 1987;
		final long height = 1441;
		final long depth = 460;
		
		final double resXY = 5.6;
		final double resZ = 11.2;
		
		final String baseUrl = "file:/home/saalfeld/tmp/catmaid/export-test/fib/aligned/xy/";
		
		final int tileWidth = 256;
		final int tileHeight = 256;
		
		final VolatileCATMAIDRandomAccessibleInterval source =
				new VolatileCATMAIDRandomAccessibleInterval(
						baseUrl,
						width,
						height,
						depth,
						0,
						tileWidth,
						tileHeight );
		
		source.draw( 800, 600 );
	}
}
