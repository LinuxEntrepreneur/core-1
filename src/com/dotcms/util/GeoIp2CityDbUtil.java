package com.dotcms.util;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.com.maxmind.geoip2.DatabaseReader;
import com.dotcms.repackage.com.maxmind.geoip2.exception.GeoIp2Exception;
import com.dotcms.repackage.com.maxmind.geoip2.model.CityResponse;
import com.dotcms.repackage.com.maxmind.geoip2.record.Subdivision;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * Provides utility methods to interact with the GeoIP2 API City Database. This
 * library lets developers discover geographic information about a specific IP
 * address, which is ideal for displaying content to Website users based on
 * different criteria, such as, city, state/province, country, language, postal
 * code, etc.
 * <p>
 * By default, Tomcat returns IPv6 addresses via the {@link HttpServletRequest}
 * object. This can be changed by adding the following parameter to the startup
 * command line of your Tomcat server:
 * <p>
 * {@code -Djava.net.preferIPv4Stack=true}
 * </p>
 * <p>
 * The GeoIP2 API is compatible with both IPv4 and IPv6 addresses. This
 * implementation uses a free of charge local <a
 * href="http://dev.maxmind.com/geoip/geoip2/geolite2/">GeoLite2 database</a>,
 * which is a file containing the IP-based geo-location information.
 * Administrators can download the file from the MaxMind Website and update such
 * a file as required.
 * <p>
 * For more information, please visit the <a
 * href="http://maxmind.github.io/GeoIP2-java/index.html">GeoIP2 Java API</a>
 * page.
 * </p>
 * 
 * @author Jose Castro
 * @version 1.0
 * @since 04-15-2015
 * 
 */
public class GeoIp2CityDbUtil {

	private static DatabaseReader databaseReader = null;

	/**
	 * Singleton holder based on the initialization-on-demand approach.
	 *
	 */
	private static class SingletonHolder {

		private static GeoIp2CityDbUtil INSTANCE = new GeoIp2CityDbUtil(
				Config.getStringProperty("GEOIP2_CITY_DATABASE_PATH", ""));

	}

	/**
	 * Private constructor that will initialize the connection to the local
	 * GeoIP2 database.
	 * 
	 * @param databasePath
	 *            - The path to the database file in the file system.
	 * @throws DotRuntimeException
	 *             If the connection to the GeoIP2 database file could not be
	 *             established.
	 */
	private GeoIp2CityDbUtil(String databasePath) {
		if (!databasePath.startsWith("/")) {
			databasePath = Config.CONTEXT.getRealPath("/" + databasePath);
		}
		File database = new File(databasePath);
		try {
			databaseReader = new DatabaseReader.Builder(database).build();
		} catch (IOException e) {
			Logger.error(this,
					"Connection to the GeoIP2 database could not be established.");
			throw new DotRuntimeException(
					"Connection to the GeoIP2 database could not be established.",
					e);
		}
	}

	/**
	 * Returns a unique instance of the {@link GeoIp2CityDbUtil} class.
	 * 
	 * @return The {@link GeoIp2CityDbUtil} instance.
	 */
	public static GeoIp2CityDbUtil getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Retrieves the client's IP address from the {@link HttpServletRequest}
	 * object based on the different available approaches. It's worth noting
	 * that, depending on the server startup parameters, the resulting IP
	 * address can be either IPv4 or IPv6.
	 * 
	 * @param request
	 *            - The {@link HttpServletRequest} object.
	 * @return The client's IP address.
	 */
	public String getClientIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_FORWARDED_FOR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_FORWARDED");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_VIA");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("REMOTE_ADDR");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	/**
	 * Returns the ISO code of the state, province or region (referred to as
	 * "subdivision") the specified IP address belongs to. The ISO code is a one
	 * or two-character representation (depending on the country) of the name of
	 * the subdivision.
	 * 
	 * @param ipAddress
	 *            - The IP address to get information from.
	 * @return The ISO code representing the state, province, or region.
	 * @throws GeoIp2Exception
	 *             If the IP address is not present in the service database.
	 * @throws IOException
	 *             If the connection to the GeoIP2 service could not be
	 *             established, or the result object could not be created.
	 */
	public String getSubdivisionIsoCode(String ipAddress) throws IOException,
			GeoIp2Exception {
		String subdivisionCode = null;
		InetAddress inetAddress = InetAddress.getByName(ipAddress);
		CityResponse response = databaseReader.city(inetAddress);
		Subdivision subdivision = response.getMostSpecificSubdivision();
		subdivisionCode = subdivision.getIsoCode();
		return subdivisionCode;
	}

}
