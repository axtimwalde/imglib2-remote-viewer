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
package net.imglib2.remote.openconnectome;

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
import net.imglib2.remote.viewer.VolatileHierarchyProjector;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.volatiles.VolatileRealType;
import net.imglib2.ui.AbstractMultiResolutionRenderer;
import net.imglib2.ui.AffineTransformType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.RendererFactory;
import net.imglib2.view.Views;

/**
 * An {@link AbstractMultiResolutionRenderer} for a hierarchy of sources.
 * It considers rendering complete when all pixels were rendered at screen
 * scale level 0 from the optimal hierarchy source. 
 * 
 * @param <A>
 *            transform type
 *            
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
public class OpenConnectomeMultiResolutionHierarchyRenderer< A extends AffineSet & AffineGet & Concatenable< AffineGet > >
		extends AbstractMultiResolutionRenderer< A >  
{
	static public enum Interpolation
	{
		NEAREST_NEIGHBOR, N_LINEAR
	}
	
	public static class Factory< B extends AffineSet & AffineGet & Concatenable< AffineGet > > implements RendererFactory< B >
	{
		final protected AffineTransformType< B > transformType;
		
		final protected InteractiveDisplayCanvasComponent< ? > canvas;
		
		final protected B sourceTransform;
		
		final protected ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > >();
		final protected ArrayList< B > sourceTransforms = new ArrayList< B >();
		final protected ArrayList< B > sourceToScreens = new ArrayList< B >();
		protected double[][] levelScales;
		
		final protected double[] screenScales;

		final protected long targetRenderNanos;

		final protected boolean doubleBuffered;

		final protected int numRenderingThreads;
		
		public Factory(
				final AffineTransformType< B > transformType,
				final InteractiveDisplayCanvasComponent< ? > canvas,
				final String baseUrl,
				final String mode,
				final long[][] levelDimensions,
				final long minZ,
				final double[][] levelScales,
				final int[][] levelCellDimensions,
				final B sourceTransform,
				final double[] screenScales,
				final long targetRenderNanos,
				final boolean doubleBuffered,
				final int numRenderingThreads )
		{
			this.transformType = transformType;
			this.canvas = canvas;
			this.sourceTransform = sourceTransform;
			this.screenScales = screenScales;
			this.targetRenderNanos = targetRenderNanos;
			this.doubleBuffered = doubleBuffered;
			this.numRenderingThreads = numRenderingThreads;
			
			setSource( baseUrl, mode, levelDimensions, minZ, levelScales, levelCellDimensions );
		}
		
		public Factory(
				final AffineTransformType< B > transformType,
				final InteractiveDisplayCanvasComponent< ? > canvas,
				final OpenConnectomeTokenInfo tokenInfo,
				final String mode,
				final B sourceTransform,
				final double[] screenScales,
				final long targetRenderNanos,
				final boolean doubleBuffered,
				final int numRenderingThreads )
		{
			this(
					transformType,
					canvas,
					tokenInfo.getBaseUrl(),
					mode,
					tokenInfo.getLevelDimensions( mode ),
					tokenInfo.getMinZ(),
					tokenInfo.getLevelScales( mode ),
//					tokenInfo.getLevelScales(),
					tokenInfo.getLevelCellDimensions(),
					sourceTransform,
					screenScales,
					targetRenderNanos,
					doubleBuffered,
					numRenderingThreads );
		}
		
		
		public void setSource(
				final String baseUrl,
				final String mode,
				final long[][] levelDimensions,
				final long minZ,
				final double[][] levelScales,
				final int[][] levelCellDimensions )
		{
			this.levelScales = new double[ levelScales.length ][];
			sourceTransforms.clear();
			sources.clear();
			sourceToScreens.clear();
			
			for ( int level = 0; level < levelScales.length; level++ )
			{
				this.levelScales[ level ] = levelScales[ level ].clone();
				final B levelTransform = transformType.createTransform();
				for ( int d = 0; d < 3; ++d )
					levelTransform.set( levelScales[ level ][ d ], d, d );
				levelTransform.set( -0.5 * ( levelScales[ level ][ 0 ] - 1 ), 0, 3 );
				levelTransform.set( -0.5 * ( levelScales[ level ][ 1 ] - 1 ), 1, 3 );
				levelTransform.set( -0.5 * ( levelScales[ level ][ 2 ] - 1 ), 2, 3 );
				
				final B sourceCopy = transformType.createTransform();
				sourceCopy.set( sourceTransform.getRowPackedCopy() );
				sourceCopy.concatenate( levelTransform );
				
				sourceTransforms.add( sourceCopy );
				
				final VolatileOpenConnectomeRandomAccessibleInterval source =
						new VolatileOpenConnectomeRandomAccessibleInterval(
							baseUrl,
							mode,
							levelDimensions[ level ][ 0 ],
							levelDimensions[ level ][ 1 ],
							levelDimensions[ level ][ 2 ],
							levelCellDimensions[ level ][ 0 ],
							levelCellDimensions[ level ][ 1 ],
							levelCellDimensions[ level ][ 2 ],
							minZ,
							level );
				
				final ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > extendedSource =
						Views.extendValue( source, new VolatileRealType< UnsignedByteType >( new UnsignedByteType( 127 ), true ) );
				sources.add( extendedSource );
				
				final B sourceToScreen = transformType.createTransform();
				sourceToScreens.add( sourceToScreen );
			}
		}
		
		@Override
		public OpenConnectomeMultiResolutionHierarchyRenderer< B > create( final RenderTarget display, final PainterThread painterThread )
		{
			final OpenConnectomeMultiResolutionHierarchyRenderer< B > renderer = new OpenConnectomeMultiResolutionHierarchyRenderer< B >(
					sources,
					sourceTransforms,
					sourceToScreens,
					levelScales,
					transformType,
					display,
					painterThread,
					screenScales,
					targetRenderNanos,
					doubleBuffered,
					numRenderingThreads );
			
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
	protected Interpolation interpolation = Interpolation.NEAREST_NEIGHBOR;
	protected InterpolatorFactory< VolatileRealType< UnsignedByteType >, RandomAccessible< VolatileRealType< UnsignedByteType > > > interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileRealType<UnsignedByteType> >();
	
	public OpenConnectomeMultiResolutionHierarchyRenderer(
			final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources,
			final ArrayList< A > sourceTransforms,
			final ArrayList< A > sourceToScreens,
			final double[][] levelScales,
			final AffineTransformType< A > transformType,
			final RenderTarget display,
			final PainterThread painterThread,
			final double[] screenScales,
			final long targetRenderNanos,
			final boolean doubleBuffered,
			final int numRenderingThreads )
	{
		super( transformType, display, painterThread, screenScales, targetRenderNanos, doubleBuffered, numRenderingThreads );
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
	
	@Override
	protected boolean isComplete()
	{
		return requestedScreenScaleIndex == 0 && ( ( VolatileHierarchyProjector< ?, ?, ? > )projector ).isValid();
	}
	
	protected synchronized void interpolateAndTransform(
			final A viewerTransform,
			final A screenScaleTransform )
	{
		transformedSources.clear();
		sourceToScreens.clear();
		
		for ( int level = getOptimalScaleIndex( viewerTransform ); level < sources.size(); level++ )
		{
			final A sourceToScreen = transformType.createTransform();
			sourceToScreen.concatenate( screenScaleTransform );
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
			final A screenScaleTransform,
			final ARGBScreenImage screenImage )
	{
		interpolateAndTransform( viewerTransform, screenScaleTransform );
		final VolatileHierarchyProjector< UnsignedByteType, VolatileRealType< UnsignedByteType >, ARGBType > p =
				new VolatileHierarchyProjector< UnsignedByteType, VolatileRealType< UnsignedByteType >, ARGBType >(
						transformedSources,
						new RealARGBConverter< VolatileRealType< UnsignedByteType > >( 0, 255 ),
						screenImage,
						Runtime.getRuntime().availableProcessors() );
//		p.clear();
		return p;
	}
	
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
