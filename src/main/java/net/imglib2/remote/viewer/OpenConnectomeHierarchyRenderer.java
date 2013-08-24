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

import java.util.ArrayList;

import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.display.RealARGBConverter;
import net.imglib2.display.VolatileRealType;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.remote.openconnectome.VolatileOpenConnectomeRandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.ui.AffineTransformType;
import net.imglib2.ui.InterruptibleProjector;
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
		
		final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > >();
		final ArrayList< B > sourceTransforms = new ArrayList< B >();
		final ArrayList< B > sourceToScreens = new ArrayList< B >();
		
		public Factory(
				final AffineTransformType< B > transformType,
				final String baseUrl,
				final long[][] levelDimensions,
				final double[][] levelScales,
				final int[][] levelCellDimensions )
		{
			this.transformType = transformType;
			
			for ( int level = 0; level < levelScales.length; level++ )
			{
				final B levelTransform = transformType.createTransform();
				for ( int d = 0; d < 3; ++d )
				{
					levelTransform.set( levelScales[ level ][ d ], d, d );
					//levelTransform.set( 0.5 * ( levelScales[ level ][ d ] - 1 ), d, 3 );
				}
				
				sourceTransforms.add( levelTransform );
				
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
						Views.extendValue( source, new VolatileRealType< UnsignedByteType >( new UnsignedByteType( 127 ) ) );
				sources.add( extendedSource );
				
				final B sourceToScreen = transformType.createTransform();
				sourceToScreens.add( sourceToScreen );
			}
		}
		
		@Override
		public OpenConnectomeHierarchyRenderer< B > create( final RenderTarget display, final PainterThread painterThread )
		{
			return new OpenConnectomeHierarchyRenderer< B >(
					sources,
					sourceTransforms,
					sourceToScreens,
					transformType,
					display,
					painterThread,
					true,
					Runtime.getRuntime().availableProcessors() );
		}		
	}
	
	/* original sources */
	final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources = new ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > >();
	final ArrayList< A > sourceTransforms = new ArrayList< A >();
	final ArrayList< A > sourceToScreens = new ArrayList< A >();
	
	/* transformed sources */
	final ArrayList< RandomAccessible< VolatileRealType< UnsignedByteType > > > transformedSources = new ArrayList< RandomAccessible< VolatileRealType< UnsignedByteType > > >();
	
	/* interpolation */
	final InterpolatorFactory< VolatileRealType< UnsignedByteType >, RandomAccessible< VolatileRealType< UnsignedByteType > > > interpolatorFactory = new NearestNeighborInterpolatorFactory< VolatileRealType<UnsignedByteType> >();
	
	public OpenConnectomeHierarchyRenderer(
			final ArrayList< ExtendedRandomAccessibleInterval< VolatileRealType< UnsignedByteType >, ? > > sources,
			final ArrayList< A > sourceTransforms,
			final ArrayList< A > sourceToScreens,
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
	}
	
	protected synchronized void interpolateAndTransform( final A viewerTransform )
	{
		transformedSources.clear();
		sourceToScreens.clear();
		for ( int level = 0; level < sources.size(); level++ )
		{
			final A sourceToScreen = transformType.createTransform();
			sourceToScreen.concatenate( viewerTransform );
			sourceToScreen.concatenate( sourceTransforms.get( level ) );
			sourceToScreens.add( sourceToScreen );
			System.out.println( sourceToScreen );
			final RealRandomAccessible< VolatileRealType< UnsignedByteType > > interpolant = Views.interpolate( sources.get( level ), interpolatorFactory );
			transformedSources.add( RealViews.affine( interpolant, sourceToScreen ) );
		}
	}
	
	@Override
	protected InterruptibleProjector createProjector(
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
		p.clear();
		return p;
	}
}
