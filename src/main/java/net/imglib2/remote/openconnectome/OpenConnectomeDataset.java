package net.imglib2.remote.openconnectome;

import java.io.Serializable;
import java.util.HashMap;

public class OpenConnectomeDataset implements Serializable
{
	private static final long serialVersionUID = -8034421659782071831L;

	public HashMap< String, int[] > cube_dimension;
	public HashMap< String, long[] > imagesize;
	public HashMap< String, long[] > isotropic_slicerange;
	public long[] resolutions;
	public long[] slicerange;
	public HashMap< String, Double > zscale;
	public HashMap< String, long[] > zscaled_slicerange;
}