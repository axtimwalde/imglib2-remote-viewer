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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.imglib2.FinalInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.remote.openconnectome.OpenConnectomeMultiResolutionHierarchyRenderer;
import net.imglib2.remote.openconnectome.OpenConnectomeTokenInfo;
import net.imglib2.ui.AffineTransformType3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.TransformEventHandler3D;
import net.imglib2.ui.overlay.BoxOverlayRenderer;
import net.imglib2.ui.overlay.LogoPainter;
import net.imglib2.ui.util.Defaults;
import net.imglib2.ui.viewer.InteractiveRealViewer;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class OpenConnectomeViewer
{
	final protected JPanel toolbar;
	protected String[] tokens;
	
	/**
	 * Fetch the list of public tokens from
	 * {@linkplain http://braingraph2.cs.jhu.edu/emca/public_tokens/}
	 * 
	 * @return a list of {@link String Strings}
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public String[] fetchTokenList()
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( "http://braingraph2.cs.jhu.edu/emca/public_tokens/" );
		final String[] tokens = gson.fromJson( new InputStreamReader( url.openStream() ), String[].class );
		return tokens;
	}
	
	/**
	 * Fetch information for a token from
	 * {@linkplain http://braingraph2.cs.jhu.edu/emca/<token>/info/}.
	 * 
	 * @param token
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the token information
	 * @throws JsonSyntaxException
	 * @throws JsonIOException
	 * @throws IOException
	 */
	final static public OpenConnectomeTokenInfo fetchTokenInfo( final String token )
			throws JsonSyntaxException, JsonIOException, IOException
	{
		final Gson gson = new Gson();
		final URL url = new URL( "http://braingraph2.cs.jhu.edu/emca/" + token + "/info/" );
		return gson.fromJson( new InputStreamReader( url.openStream() ), OpenConnectomeTokenInfo.class );
	}
	
	/**
	 * Try to fetch the list of public tokens from
	 * {@linkplain http://braingraph2.cs.jhu.edu/emca/public_tokens/}.
	 * 
	 * @param maxNumTrials the maximum number of trials
	 * 
	 * @return a list of {@link String Strings} or <code>null</code> if
	 * 		<code>maxNumTrials</code> were executed without success
	 */
	final static public String[] tryFetchTokenList( final int maxNumTrials )
	{
		String[] tokens = null;
		for ( int i = 0; i < maxNumTrials && tokens == null; ++i )
		{
			try
			{
				tokens = fetchTokenList();
				break;
			}
			catch ( final Exception e ) {}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e ) {}
		}
		return tokens;
	}
	
	
	/**
	 * Try to fetch information for a token from
	 * {@linkplain http://braingraph2.cs.jhu.edu/emca/<token>/info/}.
	 * 
	 * @param token
	 * @param maxNumTrials
	 * @return an {@link OpenConnectomeTokenInfo} instance that carries the
	 * 		token information or <code>null</code> if <code>maxNumTrials</code>
	 * 		were executed without success
	 */
	final static public OpenConnectomeTokenInfo tryFetchTokenInfo( final String token, final int maxNumTrials )
	{
		OpenConnectomeTokenInfo info = null;
		for ( int i = 0; i < maxNumTrials && info == null; ++i )
		{
			try
			{
				info = fetchTokenInfo( token );
				break;
			}
			catch ( final Exception e ) {}
			try
			{
				Thread.sleep( 100 );
			}
			catch ( final InterruptedException e ) {}
		}
		return info;
	}
	
	
	public OpenConnectomeViewer() throws InterruptedException
	{
		tokens = tryFetchTokenList( 20 );
		final String mode = "neariso";
		final OpenConnectomeTokenInfo info = tryFetchTokenInfo( tokens[ 0 ], 20 );
		
		System.out.println( new Gson().toJson( info ) );
		System.out.println( new Gson().toJson( tokens ) );
		
		for ( int i = 0; i < info.getLevelScales( mode ).length; ++i )
			System.out.println(
					i + ": " +
					info.getLevelScales( mode )[ i ][ 0 ] + ", " +
					info.getLevelScales( mode )[ i ][ 1 ] + ", " +
					info.getLevelScales( mode )[ i ][ 2 ] );
		
		/* divide cell size by 2 */
		for ( final int[] levelCellDimension : info.dataset.cube_dimension.values() )
		{
			levelCellDimension[ 0 ] /= 2;
			levelCellDimension[ 1 ] /= 2;
			levelCellDimension[ 2 ] /= 2;
		}
		
		final int w = 800, h = 450;
		
		final long[][] levelDimensions = info.getLevelDimensions( mode );
		final double[][] levelScales = info.getLevelScales( mode );

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
		
		final OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D > rendererFactory =
				new OpenConnectomeMultiResolutionHierarchyRenderer.Factory< AffineTransform3D >(
						new AffineTransformType3D(),
						canvas,
						info,
						"neariso",
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
		
		toolbar = createToolbar();
		final JFrame frame = viewer.getFrame();
		final Container cp = frame.getContentPane();
		cp.add( toolbar, BorderLayout.NORTH );
		frame.pack();
		
		canvas.addHandler(
				new KeyAdapter()
				{
					@Override
					public void keyPressed( final KeyEvent e )
					{
						switch ( e.getKeyCode() )
						{
						case KeyEvent.VK_ESCAPE:
							System.exit( 0 );
							break;
						case KeyEvent.VK_T:
							toggleToolbar();
							break;
						}
					}
				} );
		
		frame.addWindowListener(
				new WindowAdapter()
				{
					@Override
					public void windowClosing( final WindowEvent e )
					{
						System.exit( 0 );
					}
				} );
		
		viewer.requestRepaint();
	}
	
	
	final public JPanel createToolbar()
	{
		final JPanel panel = new JPanel();
		panel.setAlignmentX( Component.LEFT_ALIGNMENT );
		final JComboBox tokenList = new JComboBox( tokens );
		
		tokenList.setSelectedIndex( 0 );
//        tokenList.addActionListener( this );
		
		final JButton test = new JButton( "Button" );
		panel.add( tokenList );
		panel.add( Box.createHorizontalGlue() );
		panel.add( test );
		return panel;
	}
	
	final public void toggleToolbar()
	{
		toolbar.setVisible( !toolbar.isVisible() );
	}
	
	final static public void main( final String[] args ) throws InterruptedException
	{
		final Gson gson = new Gson();
		final OpenConnectomeViewer openConnectomeViewer = new OpenConnectomeViewer();
	}
}
