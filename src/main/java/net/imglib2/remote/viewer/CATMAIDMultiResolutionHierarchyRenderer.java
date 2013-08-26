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
import net.imglib2.converter.Converter;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.VolatileNumericType;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.remote.catmaid.VolatileCATMAIDRandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
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
public class CATMAIDMultiResolutionHierarchyRenderer< A extends AffineSet & AffineGet & Concatenable< AffineGet > >
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
		
		final ArrayList< ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > >();
		final ArrayList< B > sourceTransforms = new ArrayList< B >();
		final ArrayList< B > sourceToScreens = new ArrayList< B >();
		final double[] levelScales;
		
		final protected double[] screenScales;

		final protected long targetRenderNanos;

		final protected boolean doubleBuffered;

		final protected int numRenderingThreads;
		
		public Factory(
				final AffineTransformType< B > transformType,
				final InteractiveDisplayCanvasComponent< ? > canvas,
				final String baseUrl,
				final long width,
				final long height,
				final long depth,
				final int tileWidth,
				final int tileHeight,
				final B sourceTransform,
				final double[] screenScales,
				final long targetRenderNanos,
				final boolean doubleBuffered,
				final int numRenderingThreads )
		{
			this.transformType = transformType;
			this.canvas = canvas;
			this.screenScales = screenScales;
			this.targetRenderNanos = targetRenderNanos;
			this.doubleBuffered = doubleBuffered;
			this.numRenderingThreads = numRenderingThreads;
			this.levelScales = new double[ 3 ];
			
			for ( int level = 0; level < levelScales.length; level++ )
			{
				this.levelScales[ level ] = Math.pow( 2, level );
				final B levelTransform = transformType.createTransform();
				for ( int d = 0; d < 3; ++d )
					levelTransform.set( levelScales[ level ], d, d );
				levelTransform.set( -0.5 * ( levelScales[ level ] - 1 ), 0, 3 );
				levelTransform.set( -0.5 * ( levelScales[ level ] - 1 ), 1, 3 );
				
				final B sourceCopy = transformType.createTransform();
				sourceCopy.set( sourceTransform.getRowPackedCopy() );
				sourceCopy.concatenate( levelTransform );
				
				sourceTransforms.add( sourceCopy );
				
				final VolatileCATMAIDRandomAccessibleInterval source = new VolatileCATMAIDRandomAccessibleInterval(
						baseUrl,
						width,
						height,
						depth,
						tileWidth,
						tileHeight,
						level );
				
				final ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > extendedSource =
						Views.extendValue( source, new VolatileNumericType< ARGBType >( new ARGBType( 0xff0000c0 ), true ) );
				sources.add( extendedSource );
				
				final B sourceToScreen = transformType.createTransform();
				sourceToScreens.add( sourceToScreen );
			}
		}
		
		@Override
		public CATMAIDMultiResolutionHierarchyRenderer< B > create( final RenderTarget display, final PainterThread painterThread )
		{
			final CATMAIDMultiResolutionHierarchyRenderer< B > renderer = new CATMAIDMultiResolutionHierarchyRenderer< B >(
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
	final protected ArrayList< ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > >();
	final protected ArrayList< A > sourceTransforms = new ArrayList< A >();
	final protected ArrayList< A > sourceToScreens = new ArrayList< A >();
	final double[] levelScales;
	
	/* transformed sources */
	final protected ArrayList< RandomAccessible< VolatileNumericType< ARGBType > > > transformedSources = new ArrayList< RandomAccessible< VolatileNumericType< ARGBType > > >();
	
	/* interpolation */
	protected Interpolation interpolation = Interpolation.NEAREST_NEIGHBOR;
	protected InterpolatorFactory< VolatileNumericType< ARGBType >, RandomAccessible< VolatileNumericType< ARGBType > > > interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileNumericType< ARGBType > >();
	
	public CATMAIDMultiResolutionHierarchyRenderer(
			final ArrayList< ExtendedRandomAccessibleInterval< VolatileNumericType< ARGBType >, ? > > sources,
			final ArrayList< A > sourceTransforms,
			final ArrayList< A > sourceToScreens,
			final double[] levelScales,
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
			final double sx = levelScales[ i ];
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
			final RealRandomAccessible< VolatileNumericType< ARGBType > > interpolant = Views.interpolate( sources.get( level ), interpolatorFactory );
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
		
		final VolatileHierarchyProjector< ARGBType, VolatileNumericType< ARGBType >, ARGBType > p =
				new VolatileHierarchyProjector< ARGBType, VolatileNumericType< ARGBType >, ARGBType >(
						transformedSources,
						new Converter< VolatileNumericType< ARGBType >, ARGBType >()
						{
							@Override
							public void convert( final VolatileNumericType< ARGBType > input, final ARGBType output )
							{
								output.set( input.get() );
							}
						},
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
			interpolatorFactory = new NLinearInterpolatorFactory< VolatileNumericType< ARGBType > >();
		}
		else
		{
			interpolation = Interpolation.NEAREST_NEIGHBOR;
			interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileNumericType< ARGBType > >();
		}
	}
}
