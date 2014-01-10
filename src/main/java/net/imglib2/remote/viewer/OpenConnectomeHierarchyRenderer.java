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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.remote.openconnectome.VolatileOpenConnectomeRandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileRealType;
import net.imglib2.ui.AffineTransformType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.RendererFactory;
import net.imglib2.view.Views;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class OpenConnectomeHierarchyRenderer< A extends AffineSet & AffineGet & Concatenable< AffineGet > >
		extends AbstractHierarchyRenderer< A >  
{
	public static class Factory< B extends AffineSet & AffineGet & Concatenable< AffineGet > > implements RendererFactory< B >
	{
		final protected AffineTransformType< B > transformType;
		
		final protected InteractiveDisplayCanvasComponent< ? > canvas;
		
		final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > >();
		final ArrayList< B > sourceTransforms = new ArrayList< B >();
		final ArrayList< B > sourceToScreens = new ArrayList< B >();
		final double[][] levelScales;
		
		public Factory(
				final AffineTransformType< B > transformType,
				final InteractiveDisplayCanvasComponent< ? > canvas,
				final String baseUrl,
				final long[][] levelDimensions,
				final double[][] levelScales,
				final int[][] levelCellDimensions,
				final B sourceTransform )
		{
			this.transformType = transformType;
			this.canvas = canvas;
			this.levelScales = new double[ levelScales.length ][];
			
			for ( int level = 0; level < levelScales.length; level++ )
			{
				this.levelScales[ level ] = levelScales[ level ].clone();
				final B levelTransform = transformType.createTransform();
				for ( int d = 0; d < 3; ++d )
					levelTransform.set( levelScales[ level ][ d ], d, d );
				levelTransform.set( -0.5 * ( levelScales[ level ][ 0 ] - 1 ), 0, 3 );
				levelTransform.set( -0.5 * ( levelScales[ level ][ 1 ] - 1 ), 1, 3 );
				
				final B sourceCopy = transformType.createTransform();
				sourceCopy.set( sourceTransform.getRowPackedCopy() );
				sourceCopy.concatenate( levelTransform );
				
				sourceTransforms.add( sourceCopy );
				
				final VolatileOpenConnectomeRandomAccessibleInterval source = new VolatileOpenConnectomeRandomAccessibleInterval(
						baseUrl,
						levelDimensions[ level ][ 0 ],
						levelDimensions[ level ][ 1 ],
						levelDimensions[ level ][ 2 ],
						levelCellDimensions[ level ][ 0 ],
						levelCellDimensions[ level ][ 1 ],
						levelCellDimensions[ level ][ 2 ],
						1, level );
				
				final ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > extendedSource =
						Views.extendValue( source, new VolatileRealType< UnsignedByteType >( new UnsignedByteType( 127 ), true ) );
				sources.add( extendedSource );
				
				final B sourceToScreen = transformType.createTransform();
				sourceToScreens.add( sourceToScreen );
			}
		}
		
		@Override
		public OpenConnectomeHierarchyRenderer< B > create( final RenderTarget display, final PainterThread painterThread )
		{
			final OpenConnectomeHierarchyRenderer< B > renderer = new OpenConnectomeHierarchyRenderer< B >(
					sources,
					sourceTransforms,
					sourceToScreens,
					levelScales,
					transformType,
					display,
					painterThread,
					true,
					Runtime.getRuntime().availableProcessors() );
			
			// add KeyHandler for toggling interpolation
			canvas.addHandler( new KeyAdapter() {
				@Override
				public void keyPressed( final KeyEvent e )
				{
					if ( e.getKeyCode() == KeyEvent.VK_I )
					{
						renderer.toggleInterpolation();
						renderer.requestRepaint();
					}
				}
			});
			
			return renderer;
		}		
	}
	
	/* original sources */
	final protected ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > >();
	final protected ArrayList< A > sourceTransforms = new ArrayList< A >();
	final protected ArrayList< A > sourceToScreens = new ArrayList< A >();
	final double[][] levelScales;
	
	/* transformed sources */
	final protected ArrayList< RandomAccessible< VolatileRealType< UnsignedByteType > > > transformedSources = new ArrayList< RandomAccessible< VolatileRealType< UnsignedByteType > > >();
	
	/* interpolation */
	protected InterpolatorFactory< VolatileRealType< UnsignedByteType >, RandomAccessible< VolatileRealType< UnsignedByteType > > > interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileRealType<UnsignedByteType> >();
	
	public OpenConnectomeHierarchyRenderer(
			final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources,
			final ArrayList< A > sourceTransforms,
			final ArrayList< A > sourceToScreens,
			final double[][] levelScales,
			final AffineTransformType< A > transformType,
			final RenderTarget display,
			final PainterThread painterThread,
			final boolean doubleBuffered,
			final int numRenderingThreads )
	{
		super( transformType, display, painterThread, doubleBuffered, numRenderingThreads );
		this.sources.addAll( sources );
		this.sourceTransforms.addAll( sourceTransforms );
		this.sourceToScreens.addAll( sourceToScreens );
		this.levelScales = levelScales;
	}
	
	protected synchronized int getOptimalScaleIndex( final A viewerTransform )
	{
		double screenPixelLength = 0;
		final int n = viewerTransform.numDimensions();
		for ( int d = 0; d < n; ++d )
		{
			final double x = viewerTransform.get( d, 0 );
			screenPixelLength += x * x;
		}
		screenPixelLength = 1.0 / Math.sqrt( screenPixelLength );
		
		int i;
		for ( i = 1; i < levelScales.length; ++i )
		{
			final double sx = levelScales[ i ][ 0 ];
			if ( sx > screenPixelLength )
				break;
		}
		
//		System.out.println( "optimal level = " + ( i - 1 ) + " for square scale " + screenPixelLength );
		
		return i - 1; 
	}
	
	protected synchronized void interpolateAndTransform( final A viewerTransform )
	{
		transformedSources.clear();
		sourceToScreens.clear();
		
		for ( int level = getOptimalScaleIndex( viewerTransform ); level < sources.size(); level++ )
		{
			final A sourceToScreen = transformType.createTransform();
			sourceToScreen.concatenate( viewerTransform );
			sourceToScreen.concatenate( sourceTransforms.get( level ) );
			sourceToScreens.add( sourceToScreen );
//			System.out.println( sourceToScreen );
			final RealRandomAccessible< VolatileRealType< UnsignedByteType > > interpolant = Views.interpolate( sources.get( level ), interpolatorFactory );
			transformedSources.add( RealViews.affine( interpolant, sourceToScreen ) );
		}
	}
	
	@Override
	protected VolatileHierarchyProjector< ?, ?, ? > createProjector(
			final A viewerTransform,
			final ARGBScreenImage screenImage )
	{
		interpolateAndTransform( viewerTransform );
		
		
		final VolatileHierarchyProjector< UnsignedByteType, VolatileRealType< UnsignedByteType >, ARGBType > p =
				new VolatileHierarchyProjector< UnsignedByteType, VolatileRealType< UnsignedByteType >, ARGBType >(
						transformedSources,
						new RealARGBConverter< VolatileRealType< UnsignedByteType > >( 0, 255 ),
						screenImage,
						Runtime.getRuntime().availableProcessors() );
//		p.clear();
		return p;
	}
	
	@Override
	public void toggleInterpolation()
	{
		if ( interpolation == Interpolation.NEAREST_NEIGHBOR )
		{
			interpolation = Interpolation.N_LINEAR;
			interpolatorFactory = new NLinearInterpolatorFactory< VolatileRealType<UnsignedByteType> >();
		}
		else
		{
			interpolation = Interpolation.NEAREST_NEIGHBOR;
			interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileRealType<UnsignedByteType> >();
		}
	}
}
