package aQute.bnd.service;

import java.io.*;
import java.net.*;
import java.util.*;

import aQute.bnd.version.*;

/**
 * A Repository Plugin abstract a bnd repository. This interface allows bnd to
 * find programs from their bsn and revisions from their bsn-version
 * combination. It is also possible to put revisions in a repository if the
 * repository is not read only.
 */
public interface RepositoryPlugin {
	/**
	 * Options used to steer the put operation
	 */
	class PutOptions {
		/**
		 * The <b>SHA1</b> digest of the artifact to put into the repository.<br/>
		 * <br/>
		 * When specified the digest of the <b>fetched</b> artifact will be
		 * calculated and verified against this digest, <b>before</b> putting
		 * the artifact into the repository.<br/>
		 * <br/>
		 * An exception is thrown if the specified digest and the calculated
		 * digest do not match.
		 */
		public byte[]	digest				= null;

		/**
		 * Allow the implementation to change the artifact.<br/>
		 * <br/>
		 * When set to true the implementation is allowed to change the artifact
		 * when putting it into the repository.<br/>
		 * <br/>
		 * An exception is thrown when set to false and the implementation can't
		 * put the artifact into the repository without changing it.
		 */
		public boolean	allowArtifactChange	= false;

		/**
		 * Generate a <b>SHA1</b> digest.<br/>
		 * <br/>
		 * When set to true the implementation generates a digest of the
		 * artifact as it is put into the repository and returns that digest in
		 * the result.
		 */
		public boolean	generateDigest		= false;
	}

	PutOptions	DEFAULTOPTIONS	= new PutOptions();

	/**
	 * Results returned by the put operation
	 */
	class PutResult {
		/**
		 * The artifact as it was put in the repository.<br/>
		 * <br/>
		 * This can be a URI to the artifact (when it was put into the
		 * repository), or null when the artifact was not put into the
		 * repository (for example because it was already in the repository).
		 */
		public URI		artifact	= null;

		/**
		 * The <b>SHA1</b> digest of the artifact as it was put into the
		 * repository.<br/>
		 * <br/>
		 * This will be null when {@link PutOptions#generateDigest} was null, or
		 * when {@link #artifact} is null.
		 */
		public byte[]	digest		= null;
	}

	/**
	 * Put an artifact (from the InputStream) into the repository.<br/>
	 * <br/>
	 * There is NO guarantee that the artifact on the input stream has not been
	 * modified after it's been put in the repository since that is dependent on
	 * the implementation of the repository (see
	 * {@link RepositoryPlugin.PutOptions#allowArtifactChange}).
	 * 
	 * @param stream
	 *            The input stream with the artifact
	 * @param options
	 *            The put options. See {@link RepositoryPlugin.PutOptions}, can
	 *            be {@code null}, which will then take the default options like
	 *            new PutOptions().
	 * @return The result of the put, never null. See
	 *         {@link RepositoryPlugin.PutResult}
	 * @throws Exception
	 *             When the repository root directory doesn't exist, when the
	 *             repository is read-only, when the specified checksum doesn't
	 *             match the checksum of the fetched artifact (see
	 *             {@link RepositoryPlugin.PutOptions#digest}), when the
	 *             implementation wants to modify the artifact but isn't allowed
	 *             (see {@link RepositoryPlugin.PutOptions#allowArtifactChange}
	 *             ), or when another error has occurred.
	 */
	PutResult put(InputStream stream, PutOptions options) throws Exception;

	/**
	 * Return a URL to a matching version of the given bundle.
	 * 
	 * @param bsn
	 *            Bundle-SymbolicName of the searched bundle
	 * @param version
	 *            Version requested.
	 * @return A file to the revision or null if not found
	 * @throws Exception
	 *             when anything goes wrong
	 */
	File get(String bsn, Version version, Map<String,String> properties) throws Exception;

	/**
	 * Answer if this repository can be used to store files.
	 * 
	 * @return true if writable
	 */
	boolean canWrite();

	/**
	 * Return a list of bsns that are present in the repository.
	 * 
	 * @param regex
	 *            if not null, match against the bsn and if matches, return
	 *            otherwise skip
	 * @return A list of bsns that match the regex parameter or all if regex is
	 *         null
	 * @throws Exception
	 */
	List<String> list(String regex) throws Exception;

	/**
	 * Return a list of versions.
	 * 
	 * @throws Exception
	 */

	List<Version> versions(String bsn) throws Exception;

	/**
	 * @return The name of the repository
	 */
	String getName();

	/**
	 * Return a location identifier of this repository
	 */

	String getLocation();
}
