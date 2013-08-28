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

package net.imglib2.remote.viewer;

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
		viewer.requestRepaint();
	}
}
