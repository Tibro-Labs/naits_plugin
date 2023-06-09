package naits_triglav_plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.conveyal.data.geobuf.GeobufEncoder;
import com.conveyal.data.geobuf.GeobufFeature;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import geobuf.Geobuf;

/**
 * Svarog extension of GeobufEncoder <br>
 *
 */
public class SvGeobufEncoder extends GeobufEncoder {

	static final Logger log4j = LogManager.getLogger(WsGeometry.class.getName());

	private OutputStream outputStream;

	private Boolean includeGeometries = false;

	/**
	 * Default Constructor
	 * 
	 * @param outputStream
	 * @param precision
	 */
	public SvGeobufEncoder(OutputStream outputStream, int precision) {

		super(outputStream, precision);
		this.outputStream = outputStream;
	}

	public SvGeobufEncoder(OutputStream outputStream, int precision, boolean includeGeometries) {

		super(outputStream, precision);
		this.outputStream = outputStream;
		this.includeGeometries = includeGeometries;
	}

	/**
	 * 
	 * @param g
	 * @param metaData
	 * @return
	 */
	public GeobufFeature createGeobufFeature(Geometry g, Object userData) {

		GeobufFeature feat = new GeobufFeature();
		feat.geometry = g; // should we allow feat.geometry: null ?
		feat.properties = new HashMap<>();

		if (userData instanceof DbDataObject && userData != null) {
			DbDataObject dbo = (DbDataObject) userData;
			feat.id = dbo.getObject_id().toString();

			if (dbo.getParent_id() != null)
				feat.properties.put("parent_id", dbo.getParent_id().toString());

			// Assign default type descriptor if not specified already
			String desc = (String) dbo.getVal("DESCRIPTOR");
			if (desc == null)
				dbo.setVal("DESCRIPTOR", SvCore.getDbt(dbo.getObject_type()).getVal("TABLE_NAME"));

			// Set feature properties
			dbo.getValuesMap().forEach((k, v) -> {
				if (v != null) // should we send null vallues flying over the
								// internet?
					feat.properties.put(k.toString(), v);
			});
		}

		return feat;
	}

	private Collection<GeobufFeature> dbDataArrayToGeobuf(DbDataArray dbArr) {
		Collection<GeobufFeature> gbfc = new ArrayList<GeobufFeature>();
		Geometry g = null;

		for (DbDataObject dbo : dbArr.getItems()) {
			if (SvGeometry.hasGeometries(dbo.getObject_type())) {
				g = SvGeometry.getGeometry(dbo);
			}

			gbfc.add(createGeobufFeature(g, dbo));
		}

		return gbfc;
	}

	/**
	 * 
	 * @param ls
	 * @return
	 */
	private Geobuf.Data.Geometry lineStringToGeobuf(LineString ls) {
		Geobuf.Data.Geometry.Builder builder = Geobuf.Data.Geometry.newBuilder();
		builder.setType(Geobuf.Data.Geometry.Type.LINESTRING);

		long x, y, prevX = 0, prevY = 0;

		for (int i = 0; i < ls.getNumPoints(); i++) {
			// delta code, roundoff errors do not accumulate
			Coordinate cd = ls.getCoordinateN(i);
			x = (long) (cd.x * super.precisionMultiplier);
			y = (long) (cd.y * super.precisionMultiplier);

			builder.addCoords(x - prevX);
			builder.addCoords(y - prevY);

			prevX = x;
			prevY = y;
		}

		return builder.build();
	}

	/**
	 * 
	 * @param geom
	 * @return
	 */
	private Geobuf.Data.Geometry buildGeometry(Geometry geom) {
		if (geom instanceof Point)
			return pointToGeobuf((Point) geom);
		else if (geom instanceof LineString)
			return lineStringToGeobuf((LineString) geom);
		else if (geom instanceof Polygon)
			return polyToGeobuf((Polygon) geom);
		else if (geom instanceof MultiPolygon)
			return multiPolyToGeobuf((MultiPolygon) geom);
		else
			throw new UnsupportedOperationException("Unsupported geometry type " + geom.getGeometryType());

	}

	/**
	 * 
	 * @param featBld
	 * @param feature
	 * @param keys
	 */
	private void setProperties(Geobuf.Data.Feature.Builder featBld, GeobufFeature feature, List<String> keys) {

		for (Map.Entry<String, Object> e : feature.properties.entrySet()) {
			// TODO store keys separately from features
			Geobuf.Data.Value.Builder val = Geobuf.Data.Value.newBuilder();

			Object featVal = e.getValue();

			// init encoder with geometry boolean to include geometry data as
			// string into feature.properties map
			// geometry data excluded by default, makes objects lighter,
			// feature.geometry already contains the data
			if (featVal instanceof Geometry) {
				if (this.includeGeometries)
					val.setStringValue(featVal.toString());
				else
					continue;
			} else if (featVal instanceof String)
				val.setStringValue((String) featVal);
			else if (featVal instanceof Boolean)
				val.setBoolValue((Boolean) featVal);
			else if (featVal instanceof Integer) {
				int keyInt = (Integer) featVal;
				if (keyInt >= 0)
					val.setPosIntValue(keyInt);
				else
					val.setNegIntValue(keyInt);
			} else if (featVal instanceof Long) {
				long keyLong = (Long) featVal;
				if (keyLong >= 0)
					val.setPosIntValue(keyLong);
				else
					val.setNegIntValue(keyLong);
			} else if (featVal instanceof Double || featVal instanceof Float) // BigDecimal?
				val.setDoubleValue(((Number) featVal).doubleValue());
			else {
				// TODO serialize to JSON
				if (log4j.isDebugEnabled()) {
					log4j.debug("Unable to save object of type " + featVal.getClass().getTypeName()
							+ " to geobuf, falling back on toString. Deserialization will not work as expected.");
					val.setStringValue(featVal.toString());
				}
			}

			int keyIdx = keys.indexOf(e.getKey());
			if (keyIdx == -1) {
				synchronized (keys) {
					keyIdx = keys.size();
					keys.add(e.getKey());
				}
			}

			// properties is a jagged array of [key index, value index, . . .]
			featBld.addProperties(keyIdx);
			featBld.addProperties(featBld.getValuesCount());
			featBld.addValues(val);
		}
	}

	/**
	 * 
	 * @param featBld
	 * @param feature
	 */
	private void setId(Geobuf.Data.Feature.Builder featBld, GeobufFeature feature) {

		if (feature.id != null) {
			featBld.setId(feature.id);
			featBld.clearIntId();
		} else {
			featBld.setIntId(feature.numericId);
			featBld.clearId();
		}
	}

	/**
	 * 
	 * @param feature
	 * @param keys
	 * @return
	 */
	private Geobuf.Data.Feature buildFeature(GeobufFeature feature, List<String> keys) {

		Geobuf.Data.Feature.Builder featBld = Geobuf.Data.Feature.newBuilder();

		featBld.setGeometry(this.buildGeometry(feature.geometry));
		this.setProperties(featBld, feature, keys);
		this.setId(featBld, feature);

		return featBld.build();
	}

	/**
	 * 
	 * @param featureCollection
	 * @return
	 */
	private Geobuf.Data buildData(Collection<GeobufFeature> featureCollection) {

		Geobuf.Data.Builder data = Geobuf.Data.newBuilder().setPrecision(super.precision).setDimensions(2);

		Geobuf.Data.FeatureCollection.Builder fc = Geobuf.Data.FeatureCollection.newBuilder();

		// deduplicate keys
		List<String> keys = new ArrayList<>();

		featureCollection.stream().map(f -> this.buildFeature(f, keys)).forEach(fc::addFeatures);

		fc.addAllValues(Collections.emptyList());
		fc.addAllCustomProperties(Collections.emptyList());

		data.setFeatureCollection(fc);
		data.addAllKeys(keys);

		return data.build();
	}

	/**
	 * 
	 * @param featureCollection
	 * @return
	 */
	@Override
	public void writeFeatureCollection(Collection<GeobufFeature> featureCollection) throws IOException {

		outputStream.write(buildData(featureCollection).toByteArray());
	}

	/**
	 * wip, look elsewhere <br>
	 * how to map custom geometry (custom field name/manual construction/null)
	 * to dbDataObject values map ?
	 * 
	 * @param dbArr
	 * @throws IOException
	 */
	public void writeDbDataArray(DbDataArray dbArr) throws IOException {

		this.writeFeatureCollection(dbDataArrayToGeobuf(dbArr));
	}

	/**
	 * wip, look elsewhere
	 * 
	 * @param dbo
	 * @throws IOException
	 */
	public void writeDbDataObject(DbDataObject dbo) throws IOException {

		DbDataArray dbArr = new DbDataArray();
		dbArr.addDataItem(dbo);

		this.writeDbDataArray(dbArr);
	}

	/**
	 * 
	 * @param geomArr
	 * @throws IOException
	 */
	public void writeSvGeometry(Collection<Geometry> geomArr) throws IOException {

		Collection<GeobufFeature> gbfc = new ArrayList<GeobufFeature>();
		geomArr.forEach(g -> gbfc.add(createGeobufFeature(g, g.getUserData())));

		this.writeFeatureCollection(gbfc);
	}

}