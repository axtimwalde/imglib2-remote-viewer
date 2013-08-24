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
import net.imglib2.display.VolatileRealType;
import net.imglib2.display.VolatileRealTypeARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.ui.AffineTransformType3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.overlay.BoxOverlayRenderer;
import net.imglib2.ui.overlay.LogoPainter;
import net.imglib2.ui.viewer.InteractiveRealViewer;

public class OpenConnectomeViewer
{
	final static protected long[][] levelDimensions = new long[][]{
			{ 21504, 26624, 1850 },
			{ 10752, 13312, 1850 },
			{ 5376, 6656, 1850 },
			{ 2816, 3328, 1850 },
			{ 1536, 1792, 1850 },
			{ 768, 1024, 1850 },
			{ 512, 512, 1850 },
			{ 256, 256, 1850 } };
	
	final static protected double[][] levelScales = new double[][]{
			{ 1, 1, 10 },
			{ 2, 2, 10 },
			{ 4, 4, 10 },
			{ 8, 8, 10 },
			{ 16, 16, 10 },
			{ 32, 32, 10 },
			{ 64, 64, 10 },
			{ 128, 128, 10 } };
	
//	final static protected int[][] levelCellDimensions = new int[][]{
//			{ 128, 128, 16 },
//			{ 128, 128, 16 },
//			{ 128, 128, 16 },
//			{ 128, 128, 16 },
//			{ 128, 128, 16 },
//			{ 128, 128, 16 },
//			{ 64, 64, 64 },
//			{ 64, 64, 64 } };
	
	final static protected int[][] levelCellDimensions = new int[][]{
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 },
		{ 128, 128, 1 } };
	
	final static public void main( final String[] args )
	{
		final int w = 800, h = 450;

		final double s = w / 4;
		final AffineTransform3D initial = new AffineTransform3D();
		initial.set(
			1.0, 0.0, 0.0, -levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] / 2.0,
			0.0, 1.0, 0.0, -levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] / 2.0,
			0.0, 0.0, 1.0, -levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] / 2.0 );

		final FinalInterval sourceInterval = new FinalInterval(
				( long )Math.round( levelDimensions[ 0 ][ 0 ] * levelScales[ 0 ][ 0 ] ),
				( long )Math.round( levelDimensions[ 0 ][ 1 ] * levelScales[ 0 ][ 1 ] ),
				( long )Math.round( levelDimensions[ 0 ][ 2 ] * levelScales[ 0 ][ 2 ] ) );
		
		/* Renderer */
		final OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
				new OpenConnectomeHierarchyRenderer.Factory< AffineTransform3D >(
						new AffineTransformType3D(),
						"http://openconnecto.me/emca/kasthuri11",
						levelDimensions,
						levelScales,
						levelCellDimensions );
		
		final VolatileRealTypeARGBConverter converter = new VolatileRealTypeARGBConverter( 0, 255 );
		final InteractiveRealViewer< VolatileRealType< UnsignedByteType >, AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > > viewer =
				new InteractiveRealViewer< VolatileRealType< UnsignedByteType >, AffineTransform3D, InteractiveDisplayCanvasComponent< AffineTransform3D > >(
						AffineTransformType3D.instance,
						new InteractiveDisplayCanvasComponent< AffineTransform3D >( w, h, TransformEventHandler3D.factory() ),
						rendererFactory );

		final BoxOverlayRenderer box = new BoxOverlayRenderer( w, h );
		box.setSource( sourceInterval, initial );
		viewer.getDisplayCanvas().addTransformListener( box );
		viewer.getDisplayCanvas().addOverlayRenderer( box );
		viewer.getDisplayCanvas().addOverlayRenderer( new LogoPainter() );
		viewer.requestRepaint();
	}
}
