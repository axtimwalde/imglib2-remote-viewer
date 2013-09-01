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

import net.imglib2.FinalInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.AffineTransformType3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.overlay.BoxOverlayRenderer;
import net.imglib2.ui.overlay.LogoPainter;
import net.imglib2.ui.util.Defaults;
import net.imglib2.ui.viewer.InteractiveRealViewer;

public class OpenConnectomeViewer
{
//	final static protected String baseUrl = "http://openconnecto.me/emca/kasthuri11";
//	
//	final static protected long minZ = 1;
//	
//	final static protected long[][] levelDimensions = new long[][]{
//			{ 21504, 26624, 1850 },
//			{ 10752, 13312, 1850 },
//			{ 5376, 6656, 1850 },
////			{ 2816, 3328, 1850 },
//			{ 2688, 3328, 1850 },
////			{ 1536, 1792, 1850 },
//			{ 1408, 1664, 1850 },
////			{ 768, 1024, 1850 },
//			{ 704, 832, 1850 },
////			{ 512, 512, 1850 },
//			{ 384, 448, 1850 },
////			{ 256, 256, 1850 } };
//			{ 192, 256, 1850 } };
//	
//	final static protected double[][] levelScales = new double[][]{
//			{ 1, 1, 10 },
//			{ 2, 2, 10 },
//			{ 4, 4, 10 },
//			{ 8, 8, 10 },
//			{ 16, 16, 10 },
//			{ 32, 32, 10 },
//			{ 64, 64, 10 },
//			{ 128, 128, 10 } };
//	
////	final static protected int[][] levelCellDimensions = new int[][]{
////			{ 128, 128, 16 },
////			{ 128, 128, 16 },
////			{ 128, 128, 16 },
////			{ 128, 128, 16 },
////			{ 128, 128, 16 },
////			{ 128, 128, 16 },
////			{ 64, 64, 64 },
////			{ 64, 64, 64 } };
//	
//	final static protected int[][] levelCellDimensions = new int[][]{
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 },
//		{ 64, 64, 8 } };
	
	final static protected String baseUrl = "http://openconnecto.me/emca/bock11";
	
	final static protected long[][] levelDimensions = new long[][]{
		{ 135424, 119808, 1239 },
//		{ 67840, 59904, 1239 },
		{ 67712, 59904, 1239 },
//		{ 34048, 29952, 1239 },
		{ 33920, 29952, 1239 },
//		{ 17152, 15104, 1239 },
		{ 17024, 14976, 1239 },
//		{ 8704, 7680, 1239 },
		{ 8576, 7552, 1239 },
		{ 4352, 3840, 1239 },
//		{ 2304, 2048, 1239 },
		{ 2176, 1920, 1239 },
//		{ 1280, 1024, 1239 },
		{ 1088, 960, 1239 },
//		{ 768, 768, 1239 },
		{ 576, 512, 1239 },
//		{ 512, 512, 1239 },
		{ 320, 256, 1239 },
//		{ 256, 256, 1239 } };
		{ 192, 128, 1239 } };
	
	final static protected long minZ = 2917;
	
	final static protected double[][] levelScales = new double[][]{
		{ 1, 1, 10 },
		{ 2, 2, 10 },
		{ 4, 4, 10 },
		{ 8, 8, 10 },
		{ 16, 16, 10 },
		{ 32, 32, 10 },
		{ 64, 64, 10 },
		{ 128, 128, 10 },
		{ 256, 256, 10 },
		{ 512, 512, 10 },
		{ 1024, 1024, 10 } };
	
	final static protected int[][] levelCellDimensions = new int[][]{
		{ 64, 64, 8 },
		{ 64, 64, 8 },
		{ 64, 64, 8 },
		{ 64, 64, 8 },
		{ 64, 64, 8 },
		{ 64, 64, 4 },
		{ 64, 64, 4 },
		{ 64, 64, 4 },
		{ 64, 64, 2 },
		{ 64, 64, 2 },
		{ 64, 64, 2 } };
	
//	final static public void main( final String[] args )
//	{
//		final int w = 800, h = 450;
//
//		final AffineTransform3D initial = new AffineTransform3D();
//		initial.set(
//			1.0, 0.0, 0.0, -levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] / 2.0,
//			0.0, 1.0, 0.0, -levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] / 2.0,
//			0.0, 0.0, 1.0, -levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] / 2.0 );
//
//		final FinalInterval sourceInterval = new FinalInterval(
//				( long )Math.round( levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] ),
//				( long )Math.round( levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] ),
//				( long )Math.round( levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] ) );
//		
//		/* interactive canvas */
//		final InteractiveDisplayCanvasComponent< AffineTransform3D > canvas =
//				new InteractiveDisplayCanvasComponent< AffineTransform3D >( w, h, TransformEventHandler3D.factory() );
//		
//		/* renderer */
////		final OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
////				new OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D >(
////						new AffineTransformType3D(),
////						canvas,
////						"http://openconnecto.me/emca/kasthuri11",
////						levelDimensions,
////						levelScales,
////						levelCellDimensions,
////						initial );
//		final OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
//				new OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D >(
//						new AffineTransformType3D(),
//						canvas,
//						"http://openconnecto.me/emca/kasthuri11",
//						levelDimensions,
//						levelScales,
//						levelCellDimensions,
//						initial,
////						new double[]{ 1, 0.5, 0.25, 0.125, 0.0625, 0.03125 },
//						Defaults.screenScales,
//						Defaults.targetRenderNanos,
//						Defaults.doubleBuffered,
//						Defaults.numRenderingThreads );
//		
//		
//		final InteractiveRealViewer< AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > > viewer =
//				new InteractiveRealViewer< AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > >(
//						AffineTransformType3D.instance,
//						canvas,
//						rendererFactory );
//
//		final BoxOverlayRenderer box = new BoxOverlayRenderer( w, h );
//		box.setSource( sourceInterval, initial );
//		viewer.getDisplayCanvas().addTransformListener( box );
//		viewer.getDisplayCanvas().addOverlayRenderer( box );
//		viewer.getDisplayCanvas().addOverlayRenderer( new LogoPainter() );
//		viewer.requestRepaint();
//	}
	
	final static public void main( final String[] args )
	{
		final int w = 800, h = 450;

		final AffineTransform3D initial = new AffineTransform3D();
		initial.set(
			1.0, 0.0, 0.0, -levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] / 2.0,
			0.0, 1.0, 0.0, -levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] / 2.0,
			0.0, 0.0, 1.0, -levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] / 2.0 );

		final FinalInterval sourceInterval = new FinalInterval(
				( long )Math.round( levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] ),
				( long )Math.round( levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] ),
				( long )Math.round( levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] ) );
		
		/* interactive canvas */
		final InteractiveDisplayCanvasComponent< AffineTransform3D > canvas =
				new InteractiveDisplayCanvasComponent< AffineTransform3D >( w, h, TransformEventHandler3D.factory() );
		
		/* renderer */
//		final OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
//				new OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D >(
//						new AffineTransformType3D(),
//						canvas,
//						"http://openconnecto.me/emca/kasthuri11",
//						levelDimensions,
//						levelScales,
//						levelCellDimensions,
//						initial );
		final OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
				new OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D >(
						new AffineTransformType3D(),
						canvas,
						baseUrl,
						levelDimensions,
						minZ,
						levelScales,
						levelCellDimensions,
						initial,
//						new double[]{ 1, 0.5, 0.25, 0.125, 0.0625, 0.03125 },
						Defaults.screenScales,
						Defaults.targetRenderNanos,
						Defaults.doubleBuffered,
						Defaults.numRenderingThreads );
		
		
		final InteractiveRealViewer< AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > > viewer =
				new InteractiveRealViewer< AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > >(
						AffineTransformType3D.instance,
						canvas,
						rendererFactory );

		final BoxOverlayRenderer box = new BoxOverlayRenderer( w, h );
		box.setSource( sourceInterval, initial );
		viewer.getDisplayCanvas().addTransformListener( box );
		viewer.getDisplayCanvas().addOverlayRenderer( box );
		viewer.getDisplayCanvas().addOverlayRenderer( new LogoPainter() );
		
		canvas.addHandler(
				new KeyAdapter()
				{
					@Override
					public void keyPressed( final KeyEvent e )
					{
						if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
						{
							System.exit( 0 );
						}
					}
				} );
		
		viewer.requestRepaint();
	}
}
