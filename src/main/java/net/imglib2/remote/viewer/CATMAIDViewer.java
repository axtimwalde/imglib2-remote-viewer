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

public class CATMAIDViewer
{
	final static protected long width = 1987;
	final static protected long height = 1441;
	final static protected long depth = 460;
	
	final static protected double resXY = 5.6;
	final static protected double resZ = 11.2;
	
//	final static protected String baseUrl = "http://fly.mpi-cbg.de/map/fib/aligned/xy/";
	final static protected String baseUrl = "file:/home/saalfeld/tmp/catmaid/export-test/fib/aligned/xy/";
	
	final static protected int tileWidth = 256;
	final static protected int tileHeight = 256;
	
	final static public void main( final String[] args )
	{
		final int w = 800, h = 450;

		final AffineTransform3D initial = new AffineTransform3D();
		initial.set(
			1.0, 0.0, 0.0, -width / 2.0,
			0.0, 1.0, 0.0, -height / 2.0,
			0.0, 0.0, 1.0, -depth * resZ / resXY / 2.0 );

		final FinalInterval sourceInterval = new FinalInterval(
				width,
				height,
				( long )( depth * resZ / resXY ) );
		
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
		final CATMAIDMultiResolutionHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
				new CATMAIDMultiResolutionHierarchyRenderer.Factory< AffineTransform3D >(
						new AffineTransformType3D(),
						canvas,
						baseUrl,
						width,
						height,
						depth,
						resZ / resXY,
						tileWidth,
						tileHeight,
						initial,
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
