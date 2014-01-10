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

import java.awt.image.BufferedImage;

import net.imglib2.concatenate.Concatenable;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineSet;
import net.imglib2.ui.AbstractRenderer;
import net.imglib2.ui.AffineTransformType;
import net.imglib2.ui.InterruptibleProjector;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.util.GuiUtil;

/**
 * 
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 */
abstract public class AbstractHierarchyRenderer< A extends AffineSet & AffineGet & Concatenable< AffineGet > > extends AbstractRenderer< A >
{
	static public enum Interpolation
	{
		NEAREST_NEIGHBOR, N_LINEAR
	}
	
	final protected ARGBScreenImage[] screenImages;
	
	final protected BufferedImage[] bufferedImages;
	
	final protected boolean doubleBuffered;
	
	final protected int numRenderingThreads;
	
	protected InterruptibleProjector projector = null;
	
	protected Interpolation interpolation = Interpolation.NEAREST_NEIGHBOR;
	
	
	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages} and {@link #screenScaleTransforms} accordingly.
	 */
	protected synchronized boolean checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( componentW <= 0 || componentH <= 0 )
			return false;
		if (
				screenImages[ 0 ] == null || screenImages[ 0 ].dimension( 0 ) != componentW || screenImages[ 0 ].dimension( 1 ) != componentH )
		{
//			System.out.println( "resizing" );
			for ( int b = 0; b < screenImages.length; ++b )
			{
				screenImages[ b ] = new ARGBScreenImage( componentW, componentH );
				bufferedImages[ b ] = GuiUtil.getBufferedImage( screenImages[ b ] );
			}
		}
		return true;
	}
	
	public AbstractHierarchyRenderer(
			final AffineTransformType< A > transformType,
			final RenderTarget display,
			final PainterThread painterThread,
			final boolean doubleBuffered,
			final int numRenderingThreads )
	{
		super( transformType, display, painterThread );
		this.doubleBuffered = doubleBuffered;
		this.numRenderingThreads = numRenderingThreads;
		final int nImages = doubleBuffered ? 2 : 1;
		screenImages = new ARGBScreenImage[ nImages ];
		bufferedImages = new BufferedImage[ nImages ];
	}
	
	abstract protected VolatileHierarchyProjector< ?, ?, ? > createProjector(
			final A viewerTransform,
			final ARGBScreenImage screenImage );
	
	public void toggleInterpolation()
	{
		if ( interpolation == Interpolation.NEAREST_NEIGHBOR )
			interpolation = Interpolation.N_LINEAR;
		else
			interpolation = Interpolation.NEAREST_NEIGHBOR;
	}
	
	@Override
	public boolean paint( final A viewerTransform )
	{
		if ( !checkResize() )
			return false;
		
		// the corresponding ARGBScreenImage (to render to)
		final ARGBScreenImage screenImage;

		// the corresponding BufferedImage (to paint to the canvas)
		final BufferedImage bufferedImage;

		// the projector that paints to the screenImage.
		final VolatileHierarchyProjector< ?, ?, ? > p;

		synchronized( this )
		{
			screenImage = screenImages[ 0 ];
			bufferedImage = bufferedImages[ 0 ];
			p = createProjector( viewerTransform, screenImage );
			projector = p;
		}
		
		// try rendering
		final boolean success = p.map();
			
		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				display.setBufferedImage( bufferedImage );

				if ( doubleBuffered )
				{
					screenImages[ 0 ] = screenImages[ 1 ];
					screenImages[ 1 ] = screenImage;
					bufferedImages[ 0 ] = bufferedImages[ 1 ];
					bufferedImages[ 1 ] = bufferedImage;
				}
			}
		}
		
		if ( success && !p.isValid() )
			requestRepaint();

		return success;
	}
}
