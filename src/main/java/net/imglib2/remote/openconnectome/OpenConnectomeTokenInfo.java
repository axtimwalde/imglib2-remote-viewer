package net.imglib2.remote.openconnectome;

import java.io.Serializable;

public class OpenConnectomeTokenInfo implements Serializable
{
	private static final long serialVersionUID = -560051267067033900L;

	public OpenConnectomeDataset dataset;
	public OpenConnectomeProject project;
	
	public long[][] getLevelDimensions()
	{
		final long[][] levelDimensions = new long[ dataset.imagesize.size() ][ 3 ];
		
		for ( int i = 0; i < dataset.imagesize.size(); ++i )
		{
			final long[] xy = dataset.imagesize.get( new Integer( i ).toString() );
			levelDimensions[ i ][ 0 ] = xy[ 0 ];
			levelDimensions[ i ][ 1 ] = xy[ 1 ];
			levelDimensions[ i ][ 2 ] = dataset.slicerange[ 1 ] - dataset.slicerange[ 0 ];
		}
		return levelDimensions;
	}
	
	public int[][] getLevelCellDimensions()
	{
		final int[][] levelCellDimensions = new int[ dataset.cube_dimension.size() ][];
		
		for ( int i = 0; i < dataset.cube_dimension.size(); ++i )
			levelCellDimensions[ i ] = dataset.cube_dimension.get( new Integer( i ).toString() ).clone();
		return levelCellDimensions;
	}
	
	public double[][] getLevelScales()
	{
		final double[][] levelScales = new double[ dataset.zscale.size() ][ 3 ];
		
		long s = 1;
		for ( int i = 0; i < dataset.zscale.size(); ++i, s <<= 1 )
		{
			levelScales[ i ][ 0 ] = s;
			levelScales[ i ][ 1 ] = s;
			levelScales[ i ][ 2 ] = dataset.zscale.get( new Integer( i ).toString() ) * s;
		}
			
		return levelScales;
	}
	
	public String getBaseUrl()
	{
		return "http://openconnecto.me/emca/" + project.dataset;
	}
	
	public long getMinZ()
	{
		return dataset.slicerange[ 0 ];
	}
}