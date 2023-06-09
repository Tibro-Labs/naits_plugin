package naits_triglav_plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSDITile;
import com.prtech.svarog.SvUtil;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.SvCharId;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import com.vividsolutions.jts.util.GeometricShapeFactory;

@Path("/SDI")
public class WsGeometry {

	/**
	 * 
	 */
	static final Logger log4j = LogManager.getLogger(WsGeometry.class.getName());
	/**
	 * 
	 */
	static final GeometryFactory G_FACTORY = SvUtil.sdiFactory;

	public class SearchLocations extends GeoCoder {

		public SearchLocations(SvReader svr, String input, JsonObject config) {
			super(svr, input, config);
		}

		@Override
		public DbSearchExpression genSearchEx(String type, String searchStr, JsonArray critArr) throws SvException {
			DbSearchExpression dbse = new DbSearchExpression();
			// Add base search expression, config columns like input search
			// string
			DbSearchExpression dbseBase = super.genSearchEx(type, searchStr, critArr);
			dbse.addDbSearchItem(dbseBase);

			// Issues solved here:
			switch (type) {
			case Tc.SVAROG_SDI_UNITS:
				// Communities do not have geometry, search
				// regions/municipalities/villages (unit_class in (1,2,4))
				DbSearchExpression dbseU = new DbSearchExpression();
				DbSearchCriterion critU1 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "1");
				critU1.setNextCritOperand("OR");
				DbSearchCriterion critU2 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "2");
				critU2.setNextCritOperand("OR");
				DbSearchCriterion critU4 = new DbSearchCriterion(Tc.UNIT_CLASS, DbCompareOperand.EQUAL, "4");
				dbseU.addDbSearchItem(critU1).addDbSearchItem(critU2).addDbSearchItem(critU4);
				dbse.addDbSearchItem(dbseU);
				break;
			case Tc.QUARANTINE:
				// Quarantines may not have geometry.
				// Search only blacklist type.
				// Partial fix, geom may still be null, at least other Q types
				// definitely won't have a geometry.
				// Omit null items that slip here in result builder.
				DbSearchExpression dbseQ = new DbSearchExpression();
				DbSearchCriterion critQ1 = new DbSearchCriterion(Tc.QUARANTINE_TYPE, DbCompareOperand.EQUAL, "1");
				dbseQ.addDbSearchItem(critQ1);
				dbse.addDbSearchItem(dbseQ);
				break;
			default:
				// do nothing
				break;
			}

			return dbse;
		}

		@Override
		public String setMarkup(String type, JsonArray config, LinkedHashMap<SvCharId, Object> vals) {
			// namespace for custom markup implementation
			return super.setMarkup(type, config, vals);
		}

		@Override
		public Geometry setGeometry(String type, DbDataObject dbo) throws SvException {
			Geometry g = null;
			this.svr.setIncludeGeometries(true);

			switch (type) {
			case Tc.HOLDING:
				// Holdings should always be described spatially
				// If the holding object geom isNull, default to village
				// location
				g = getHoldingGeometry(this.svr, dbo);
				break;
			case Tc.ANIMAL:
				// It follows that animals should always have a location as well
				// ?
				// since animals will always reference a particular holding
				// (parent_id)
				g = getHoldingGeometry(this.svr,
						this.svr.getObjectById(dbo.getParent_id(), SvCore.getTypeIdByName(Tc.HOLDING), null));
				break;
			case Tc.QUARANTINE:
				// Quarantine may not be described spatially, no geometry=>no
				// party
				// Handle nullPointers
				DbDataArray qGeom = this.svr.getObjectsByParentId(dbo.getObject_id(),
						SvCore.getTypeIdByName(Tc.QUARANTINE_GEOMETRY), null, null, null);

				if (!qGeom.getItems().isEmpty())
					g = SvGeometry.getGeometry(qGeom.get(0));
				break;
			default:
				// SV_SDI_UNITS defaults to base getter
				// Communities are not described spatially, need solution
				// Only regions and villages have geometry
				g = SvGeometry.getGeometry(dbo);
			}

			return g;
		}
	}

	/**
	 * 
	 * @param sessionId
	 * @param objectName
	 * @param bbox
	 * @return
	 * @throws SvException
	 */
	private ArrayList<Geometry> getGeometryImpl(String sessionId, Long objTypeId, String bbox) throws SvException {
		SvGeometry svg = null;
		ArrayList<Geometry> geom = new ArrayList<Geometry>();

		try {
			svg = new SvGeometry(sessionId);
			geom = svg.getGeomtriesByBBOX(objTypeId, bbox);
		} finally {
			if (svg != null)
				svg.release();
		}

		return geom;
	}

	/**
	 * 
	 * @param g
	 * @return
	 */
	private Double getSimplifyCoefficient(Geometry g) {
		double coef = 0.1;
		double vtxCnt = g.getNumPoints();

		if (vtxCnt < 100 && vtxCnt > 10)
			coef = 10;
		if (vtxCnt > 99)
			coef = vtxCnt;

		return coef;
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	private Point createPoint(Double x, Double y) {
		return G_FACTORY.createPoint(new Coordinate(x, y));
	}

	/**
	 * 
	 * @param coords
	 * @return
	 */
	private LineString createLine(CoordinateList coords) {
		return G_FACTORY.createLineString(coords.toCoordinateArray());
	}

	/**
	 * 
	 * @param coords
	 * @return
	 */
	private Polygon createPolygon(Coordinate[] coords) {
		return G_FACTORY.createPolygon(coords);
	}

	/**
	 * 
	 * @param cnt
	 * @param r
	 * @param pN
	 * 
	 * @return Circle shape <Polygon>
	 */
	private Polygon createCircle(Point cnt, Long r, int pN) {
		GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
		shapeFactory.setNumPoints(pN);
		shapeFactory.setCentre(new Coordinate(cnt.getX(), cnt.getY()));
		shapeFactory.setSize(r * 2);

		return shapeFactory.createCircle();
	}

	/**
	 * 
	 * @param h1
	 * @param h2
	 * @return
	 * @throws SvException
	 */
	LineString createLineFromHoldings(SvReader svr, DbDataObject h1, DbDataObject h2) throws SvException {
		CoordinateList coords = new CoordinateList();
		coords.add(getHoldingGeometry(svr, h1).getCoordinate(), false);
		coords.add(getHoldingGeometry(svr, h2).getCoordinate(), false);

		return createLine(coords);
	}

	/**
	 * 
	 * @param svr
	 * @param g1
	 * @param g2
	 * @return
	 * @throws SvException
	 */
	LineString createLineFromHoldings(SvReader svr, Geometry g1, Geometry g2) throws SvException {
		return createLineFromHoldings(svr, (DbDataObject) g1.getUserData(), (DbDataObject) g2.getUserData());
	}

	/**
	 * 
	 * @param pArr
	 * @return
	 */
	Polygon createQuarantinePolygon(String[] pArr) {
		Coordinate[] coords = new Coordinate[pArr.length + 1];
		// order of points is important (list won't preserve order), what can go
		// wrong?
		for (int i = 0; i < pArr.length; i++) {
			coords[i] = new Coordinate(unstringifyPoint(pArr[i]).getCoordinate());
		}
		// close ring for a valid polygon geometry
		// take first point of set and add as last point
		coords[coords.length - 1] = new Coordinate(coords[0]);

		return createPolygon(coords);
	}

	/**
	 * 
	 * @param svr
	 *            SvCore instance
	 * @param geom
	 *            <Point> center of quarantine or Array<Point> representing a
	 *            polygon
	 * @param r
	 *            <String> radius of quarantine, if geom is circle
	 * 
	 * @return Circle shape <Polygon>
	 * @throws SvException
	 */
	Geometry createQuarantineShape(SvReader svr, String geom, String r) throws SvException {
		Geometry qShape = null;

		String geomFormat = geom.replace("),", ")-");
		String[] pArr = geomFormat.split("-");

		if (pArr.length > 0) {
			if (pArr.length > 2) {
				qShape = createQuarantinePolygon(pArr);
			} else {
				qShape = createCircle(unstringifyPoint(geom), Long.valueOf(r), 10);
			}
		}
		// Intersect with municipality
		qShape = intersectQuarantineWithSDIUnit(svr, qShape);

		if (qShape instanceof Polygon)
			qShape = G_FACTORY.createMultiPolygon(new Polygon[] { (Polygon) qShape });

		return qShape;
	}

	/**
	 * 
	 * @param svr
	 * @param qGeom
	 * @return
	 * @throws SvException
	 */
	Geometry intersectQuarantineWithSDIUnit(SvReader svr, Geometry qGeom) throws SvException {
		// Default is input shape, so we don't shoot ourselves in the foot here
		// and return nada
		Geometry result = qGeom;
		Reader reader = new Reader();
		;

		Point cnt = qGeom.getCentroid();
		// Get relevant adm units
		DbDataArray muniList = reader.getMunicipalities(svr);
		// How to handle geometry simplifier? intersection result may contains
		// high double or triple digit count of vertices...
		// Simplify too much and the shape gets too deformed, operation will
		// become useless and detrimental to result
		// i.e simplifier may create lines that are not contained in the
		// specified sdiUnit(lines are drawn in neighbours)
		for (DbDataObject muni : muniList.getItems()) {
			Geometry muniGeom = SvGeometry.getGeometry(muni);
			if (muniGeom != null && muniGeom.covers(cnt)) {
				Geometry isectResult = muniGeom.intersection(qGeom);
				result = TopologyPreservingSimplifier.simplify(isectResult, getSimplifyCoefficient(isectResult));
			}
		}

		return result;
	}

	/**
	 * 
	 * @param point
	 * @return
	 */
	protected String stringifyPoint(Point point) {
		return Double.toString(point.getX()) + "," + Double.toString(point.getY());
	}

	/**
	 * 
	 * @param point
	 *            <String> string representation of jts.Point
	 * 
	 * @return p <Point>
	 */
	protected Point unstringifyPoint(String s) {
		Point p = null;

		try {
			String[] sCoords = s.substring(6, s.length() - 1).split(", ");
			if (sCoords != null && sCoords.length > 1) {
				Double x = Double.valueOf(sCoords[0]);
				Double y = Double.valueOf(sCoords[1]);

				p = G_FACTORY.createPoint(new Coordinate(x, y));
			}
		} catch (Exception e) {
			log4j.error(e);
		}

		return p;
	}

	/**
	 * 
	 * @param svr
	 * @param unitId
	 * @return
	 * @throws SvException
	 */
	protected DbDataArray getSDIUnitById(SvReader svr, String unitId) throws SvException {
		DbSearchExpression dbse = new DbSearchExpression();
		DbSearchCriterion dbsc1 = new DbSearchCriterion(Tc.UNIT_ID, DbCompareOperand.EQUAL, unitId);
		dbse.addDbSearchItem(dbsc1);

		return svr.getObjects(dbse, svCONST.OBJECT_TYPE_SDI_UNITS, null, null, null);
	}

	/**
	 * 
	 * @param svr
	 * @param dbo
	 * @return
	 * @throws SvException
	 */
	protected Point getHoldingDefaultGeometry(SvReader svr, DbDataObject dbo) throws SvException {
		Point point = null;
		DbDataArray unit = getSDIUnitById(svr, (String) dbo.getVal(Tc.VILLAGE_CODE));

		if (unit != null)
			point = (Point) SvGeometry.getGeometry(unit.get(0));
		if (point != null)
			point.setUserData(dbo);

		return point;
	}

	/**
	 * 
	 * @param svr
	 * @param dbo
	 * @return
	 * @throws SvException
	 */
	protected Point getHoldingGeometry(SvReader svr, DbDataObject dbo) throws SvException {
		if (SvGeometry.getGeometry(dbo) != null)
			return (Point) SvGeometry.getGeometry(dbo);

		return getHoldingDefaultGeometry(svr, dbo);
	}

	/**
	 * 
	 * @param svr
	 * @param dbo
	 * @return
	 * @throws SvException
	 */
	protected Point getPetGeometry(SvReader svr, DbDataObject dbo) throws SvException {
		Point point = null;
		if (SvGeometry.getGeometry(dbo) != null)
			point = (Point) SvGeometry.getGeometry(dbo);
		if (point != null)
			point.setUserData(dbo);
		return point;
	}

	/**
	 * 
	 * @param id
	 * @param dbo
	 * @return
	 */
	protected void setDescriptor(DbDataObject dbo, String id) {
		dbo.setVal(Tc.DESCRIPTOR, id);
	}

	/**
	 * 
	 * @param id
	 * @param g
	 * @return
	 */
	protected void setDescriptor(Geometry g, String id) {
		if ((DbDataObject) g.getUserData() == null) {
			DbDataObject dbo = new DbDataObject();
			dbo.setVal(Tc.DESCRIPTOR, id);
			g.setUserData(dbo);
		} else {
			setDescriptor((DbDataObject) g.getUserData(), id);
		}
	}

	/**
	 * 
	 * @param dbo
	 * @param a
	 * @param b
	 * @return
	 */
	protected Boolean checkMovementDateFilter(DbDataObject dbo, String a, String b) {

		if (dbo.getVal(Tc.DEPARTURE_DATE) != null && dbo.getVal(Tc.ARRIVAL_DATE) != null && a != null && b != null) {
			LocalDate start = LocalDate.parse(dbo.getVal(Tc.DEPARTURE_DATE).toString());
			LocalDate end = LocalDate.parse(dbo.getVal(Tc.ARRIVAL_DATE).toString());
			LocalDate startFilter = LocalDate.parse(a);
			LocalDate endFilter = LocalDate.parse(b);
			/*
			 * System.out.println("<<<<<< Data >>>>>>");
			 * System.out.println(start); System.out.println(end);
			 * System.out.println(""); System.out.println(
			 * "<<<<<< Filters >>>>>>"); System.out.println(startFilter);
			 * System.out.println(endFilter); System.out.println("");
			 */
			if (!startFilter.isAfter(endFilter)) { // obviously
				// System.out.println("Or is it?");
				// isBefore excludes equal values
				// If there is a movement on a single day, and the user
				// explicitly filters for the same day only, object WILL NOT be
				// included

				// instead of => startFilter.isBefore(end) &&
				// endFilter.isAfter(start)
				// express via opposite statement
				if (!startFilter.isAfter(end) && !endFilter.isBefore(start)) {
					// System.out.println("Confirmation of dates within range:
					// "+start+" to "+end+" overlaps with "+startFilter+" to
					// "+endFilter);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @param dbo
	 * @param a
	 * @param b
	 * @return
	 */
	protected Boolean checkQuarantineDateFilter(DbDataObject dbo, String a, String b) {
		if (dbo.getVal(Tc.DATE_FROM) != null && dbo.getVal(Tc.DATE_TO) != null && a != null && b != null) {
			LocalDate start = LocalDate.parse(dbo.getVal(Tc.DATE_FROM).toString());
			LocalDate end = LocalDate.parse(dbo.getVal(Tc.DATE_TO).toString());
			LocalDate startFilter = LocalDate.parse(a);
			LocalDate endFilter = LocalDate.parse(b);

			if (!startFilter.isAfter(endFilter)) {
				if (!startFilter.isAfter(end) && !endFilter.isBefore(start)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * 
	 * @param bbox
	 *            <String> Bounding box
	 * 
	 * @return List<Geometry> of all holdings intersecting a quarantine bbox
	 */
	protected DbDataArray getHoldingsByQuarantine(SvReader svr, Geometry qShape) throws SvException {
		DbDataArray dbArr = null;
		Reader rdr = null;
		String token = null;
		String qBbox = null;

		try {
			rdr = new Reader();
			token = svr.getSessionId();
			dbArr = new DbDataArray();
			qBbox = SvGeometry.getBBox(qShape.getEnvelopeInternal());
			// get holdings with own geometry
			ArrayList<Geometry> hGeomArr = getGeometryImpl(token, SvCore.getTypeIdByName(Tc.HOLDING), qBbox);
			// get villages
			ArrayList<Geometry> uGeomArr = getGeometryImpl(token, svCONST.OBJECT_TYPE_SDI_UNITS, qBbox);

			// add all holdings with own geometry to result
			if (!hGeomArr.isEmpty()) {
				for (Geometry hGeom : hGeomArr) {
					DbDataObject hDbo = (DbDataObject) hGeom.getUserData();
					dbArr.addDataItem(hDbo);
				}
			}

			// add all holdings with no gps coords for each village to result
			if (!uGeomArr.isEmpty()) {
				for (Geometry uGeom : uGeomArr) {
					DbDataObject uDbo = (DbDataObject) uGeom.getUserData();
					String unitId = uDbo.getVal(Tc.UNIT_ID).toString();

					if (unitId != null) {
						DbDataArray hArr = rdr.getHoldingsByUnitId(svr, unitId);
						for (DbDataObject hDbo : hArr.getItems()) {
							dbArr.addDataItem(hDbo);
						}
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return dbArr;
	}

	/**
	 * Copies all of the mappings from the source <DbDataObject> value map to
	 * target <DbDataObject> value map <br>
	 * Source mappings will replace key value pairs of target map.
	 * 
	 * @param target
	 *            <DbDataObject>
	 * @param source
	 *            <DbDataObject>
	 * 
	 * @return target <DbDataObject>
	 */
	protected DbDataObject mergeDbValues(DbDataObject target, DbDataObject source) {
		// Should we allow null values?
		LinkedHashMap<SvCharId, Object> targetMap = target.getValuesMap();

		source.getValuesMap().forEach((k, v) -> {
			if (v != null)
				targetMap.put(k, v);
		});
		target.setValuesMap(targetMap);

		return target;
	}

	/**
	 * 
	 * Copies all of the mappings from each item in source <DbDataArray> to
	 * target <DbDataObject> value map <br>
	 * Source mappings will replace key value pairs of target map.
	 * 
	 * @param target
	 *            <DbDataObject>
	 * @param source
	 *            <DbDataArray>
	 * 
	 * @return target <DbDataObject>
	 */
	protected DbDataObject mergeDbValues(DbDataObject target, DbDataArray source) {
		source.getItems().forEach(dbo -> mergeDbValues(target, dbo));

		return target;
	}

	/**
	 * 
	 * Merges geometry metadata with dbDataObject values map
	 * 
	 * @param g
	 * @param dbo
	 * 
	 * @return g <Geometry>
	 */
	protected Geometry extendMetadata(Geometry g, DbDataObject dbo) {
		DbDataObject gData = (DbDataObject) g.getUserData();
		g.setUserData(mergeDbValues(gData, dbo));

		return g;
	}

	/**
	 * Invalidates tiles from cache which are: <br>
	 * - intersecting with g <br>
	 * - of type
	 * 
	 * @param g
	 * @param type
	 * 
	 * @return void
	 */
	protected void invalidateGeoCache(Geometry g, Long type) {
		List<Geometry> tileGeomList = null;

		if (type == null)
			type = ((DbDataObject) g.getUserData()).getObject_type();

		if (g != null && type != null) {
			Envelope env = g.getEnvelopeInternal();
			tileGeomList = SvGeometry.getTileGeomtries(env);

			for (Geometry tgl : tileGeomList) {
				String tileID = (String) tgl.getUserData();
				SvSDITile tile = SvGeometry.getTile(type, tileID, null);
				tile.setIsTileDirty(true);
			}
		}
	}

	protected void saveQuarantineGeometry(SvReader svr, SvWriter svw, DbDataObject qDbo, String center, String radius,
			Geometry qShape, boolean isUpdate) throws SvException {
		SvGeometry svg = null;
		DbDataObject qGeomDbo = null;

		try {
			svg = new SvGeometry(svw);
			qGeomDbo = new DbDataObject();
			Long qGeomType = SvCore.getTypeIdByName(Tc.QUARANTINE_GEOMETRY);
			Point cnt = unstringifyPoint(center);
			if (isUpdate) {
				qGeomDbo = svr.getObjectsByParentId(qDbo.getObject_id(), qGeomType, null, null, null, null).get(0);
				cnt = SvGeometry.getCentroid(qGeomDbo);
			}
			// assemble q geometry object
			qGeomDbo.setObject_type(qGeomType);
			qGeomDbo.setParent_id(qDbo.getObject_id());
			qGeomDbo.setVal(Tc.RADIUS, Long.valueOf(radius));
			SvGeometry.setGeometry(qGeomDbo, qShape);
			SvGeometry.setCentroid(qGeomDbo, cnt);

			svg.saveGeometry(qGeomDbo);
		} finally {
			if (svg != null)
				svg.release();
		}
	}

	/**
	 * Build movements geometry set for input holding
	 * 
	 * @param svr
	 * @param rdr
	 * @param objectId
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws SvException
	 */
	protected ArrayList<Geometry> buildMovements(SvReader svr, Reader rdr, Long objectId, String startDate,
			String endDate) throws Exception, SvException {

		ArrayList<Geometry> geomArr = new ArrayList<Geometry>();
		List<String> nodes = new ArrayList<String>();

		DbDataObject h1 = svr.getObjectById(objectId, SvCore.getTypeIdByName(Tc.HOLDING), null);
		String h1Pic = (String) h1.getVal(Tc.PIC);
		DbDataArray moveArr = rdr.getTransferMovementsByPIC(svr, h1Pic);

		if (!moveArr.getItems().isEmpty()) {
			setDescriptor(h1, Tc.MOVEMENT_HOLDING);
			Geometry h1Geom = getHoldingGeometry(svr, h1);
			// if there are no geometry-valid h2 objects
			// ws will return a single object ?
			geomArr.add(h1Geom);

			for (DbDataObject moveDbo : moveArr.getItems()) {
				if (!checkMovementDateFilter(moveDbo, startDate, endDate)) {
					continue;
				}
				String srcPic = (String) moveDbo.getVal(Tc.SOURCE_HOLDING_ID);
				String destPic = (String) moveDbo.getVal(Tc.DESTINATION_HOLDING_ID);

				DbDataObject h2 = h1Pic.equals(srcPic) ? rdr.findAppropriateHoldingByPic(destPic, svr)
						: rdr.findAppropriateHoldingByPic(srcPic, svr);
				String h2Pic = (String) h2.getVal(Tc.PIC);
				Geometry h2Geom = getHoldingGeometry(svr, h2);

				if (!nodes.contains(h2Pic) && !h1Geom.equals(h2Geom)) {
					nodes.add(h2Pic);
					LineString ls = createLineFromHoldings(svr, h1, h2);
					setDescriptor(h2, Tc.MOVEMENT_HOLDING);
					geomArr.add(getHoldingGeometry(svr, h2));
					setDescriptor(ls, Tc.MOVEMENT_LINE);
					geomArr.add(ls);
				}
			}
		}

		return geomArr;
	}

	/**
	 * Build geometries for a specific movement set, usually assembled by eartag
	 * 
	 * @param svr
	 * @param rdr
	 * @param moveArr
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	protected ArrayList<Geometry> buildMovements(SvReader svr, Reader rdr, DbDataArray moveArr, String startDate,
			String endDate) throws Exception, SvException {

		ArrayList<Geometry> geomArr = new ArrayList<Geometry>();
		List<String> nodes = new ArrayList<String>();
		for (DbDataObject moveDbo : moveArr.getItems()) {
			if (!checkMovementDateFilter(moveDbo, startDate, endDate)) {
				continue;
			}
			String srcPic = (String) moveDbo.getVal(Tc.SOURCE_HOLDING_ID);
			String destPic = (String) moveDbo.getVal(Tc.DESTINATION_HOLDING_ID);

			if (!srcPic.equals(destPic)) {
				if (!nodes.contains(srcPic + destPic) && !nodes.contains(destPic + srcPic)) {
					nodes.add(srcPic + destPic);

					Point srcGeom = getHoldingGeometry(svr, rdr.findAppropriateHoldingByPic(srcPic, svr));
					Point destGeom = getHoldingGeometry(svr, rdr.findAppropriateHoldingByPic(destPic, svr));

					if (!srcGeom.equals(destGeom)) {

						LineString ls = createLineFromHoldings(svr, srcGeom, destGeom);

						setDescriptor(ls, Tc.MOVEMENT_LINE);
						geomArr.add(ls);
						if (!geomArr.contains(srcGeom)) {
							setDescriptor(srcGeom, Tc.MOVEMENT_HOLDING);
							geomArr.add(srcGeom);
						}
						if (!geomArr.contains(destGeom)) {
							setDescriptor(destGeom, Tc.MOVEMENT_HOLDING);
							geomArr.add(destGeom);
						}
					}
				}
			}
		}

		return geomArr;
	}

	/**
	 * 
	 * @param sessionId
	 * @param objectName
	 * @param bbox
	 * @return
	 */
	@GET
	@Path("/getGeometry/bbox/{sessionId}/{objectName}/{bbox}")
	@Produces("application/pbf")
	public StreamingOutput getGeometry(@PathParam("sessionId") final String sessionId,
			@PathParam("objectName") final String objectName, @PathParam("bbox") final String bbox) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {
				SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);
				try {
					ArrayList<Geometry> geomArr = getGeometryImpl(sessionId, SvCore.getTypeIdByName(objectName), bbox);
					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching geometry set. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				}
			};
		};
	}

	/**
	 * 
	 * @param sessionId
	 * @param objectName
	 * @param bbox
	 * @return
	 */
	@GET
	@Path("/getObjectBBOX/{sessionId}/{objectName}/{bbox}")
	@Produces("application/pbf")
	public StreamingOutput getObjectBBOX(@PathParam("sessionId") final String sessionId,
			@PathParam("objectName") final String objectName, @PathParam("bbox") final String bbox) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);
				ArrayList<Geometry> geomArr = new ArrayList<Geometry>();

				try {
					ArrayList<Geometry> geoArr = getGeometryImpl(sessionId, SvCore.getTypeIdByName(objectName), bbox);
					for (Geometry g : geoArr) {
						geomArr.add(g);
					}
					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching geo object BBOX. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				}
			};
		};
	}

	/**
	 * 
	 * @param sessionId
	 * @param objectName
	 * @param bbox
	 * @return
	 */
	@GET
	@Path("/getVillages/bbox/{sessionId}/{objectName}/{bbox}")
	@Produces("application/pbf")
	public StreamingOutput getVillages(@PathParam("sessionId") final String sessionId,
			@PathParam("objectName") final String objectName, @PathParam("bbox") final String bbox) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);
				ArrayList<Geometry> geomArr = new ArrayList<Geometry>();

				try {
					ArrayList<Geometry> sdiUnits = getGeometryImpl(sessionId, SvCore.getTypeIdByName(objectName), bbox);

					for (Geometry g : sdiUnits) {
						DbDataObject metaData = (DbDataObject) g.getUserData();
						if (metaData.getVal(Tc.UNIT_CLASS).equals("4"))
							// set sdi_units type 4 descriptor if necessary
							geomArr.add(g);
					}

					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching villages. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				}
			};
		};
	}

	/**
	 * 
	 * @param sessionId
	 * @param objectName
	 * @param bbox
	 * @return
	 */
	@GET
	@Path("/getMunicipalities/bbox/{sessionId}/{objectName}/{bbox}")
	@Produces("application/pbf")
	public StreamingOutput getMunicipalities(@PathParam("sessionId") final String sessionId,
			@PathParam("objectName") final String objectName, @PathParam("bbox") final String bbox) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);
				ArrayList<Geometry> geomArr = new ArrayList<Geometry>();

				try {
					ArrayList<Geometry> sdiUnits = getGeometryImpl(sessionId, SvCore.getTypeIdByName(objectName), bbox);

					for (Geometry g : sdiUnits) {
						DbDataObject metaData = (DbDataObject) g.getUserData();
						if (metaData.getVal(Tc.UNIT_CLASS).equals("2") || metaData.getVal(Tc.UNIT_CLASS).equals("1"))
							// set sdi_units type 4 descriptor if necessary
							geomArr.add(g);
					}

					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching villages. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				}
			};
		};
	}

	/**
	 * 
	 * @param <String>
	 *            sessionId
	 * @param <Long>
	 *            objectId
	 * 
	 * @return StreamingOutput
	 */
	@GET
	@Path("/getMovements/holding/{sessionId}/{objectId}/{startDate}/{endDate}")
	@Produces("application/pbf")
	public StreamingOutput getMovements(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final Long objectId, @PathParam("startDate") final String startDate,
			@PathParam("endDate") final String endDate) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvReader svr = null;
				Reader rdr = null;

				try {
					svr = new SvReader(sessionId);
					svr.setIncludeGeometries(true);
					rdr = new Reader();
					SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);

					enc.writeSvGeometry(buildMovements(svr, rdr, objectId, startDate, endDate));
				} catch (Exception e) {
					String errMsg = "Failed fetching geometry set. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				} finally {
					if (svr != null) {
						svr.release();
					}
				}
			};
		};
	}

	/**
	 * 
	 * @param <String>
	 *            sessionId
	 * @param <String>
	 *            earTag
	 * 
	 * @return StreamingOutput
	 */
	@GET
	@Path("/getMovements/animal/{sessionId}/{earTag}/{startDate}/{endDate}")
	@Produces("application/pbf")
	public StreamingOutput getMovements(@PathParam("sessionId") final String sessionId,
			@PathParam("earTag") final String earTag, @PathParam("startDate") final String startDate,
			@PathParam("endDate") final String endDate) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvReader svr = null;
				Reader rdr = null;

				try {
					rdr = new Reader();
					svr = new SvReader(sessionId);
					svr.setIncludeGeometries(true);
					SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);

					DbDataArray moveArr = rdr.getTransferMovementsByEartag(svr, earTag);
					ArrayList<Geometry> geomArr = buildMovements(svr, rdr, moveArr, startDate, endDate);

					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching geometry set. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				} finally {
					if (svr != null) {
						svr.release();
					}
				}
			};
		};
	}

	@GET
	@Path("/getBlacklistedMovements/quarantine/{sessionId}/{objectId}/{startDate}/{expiryDate}")
	@Produces("application/pbf")
	public StreamingOutput getBlacklistedMovements(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final Long objectId, @PathParam("startDate") final String startDate,
			@PathParam("expiryDate") final String expiryDate) {
		return new StreamingOutput() {

			@Override
			public void write(OutputStream stream) throws IOException, WebApplicationException {

				SvReader svr = null;
				ArrayList<Geometry> geomArr = null;
				PreparedStatement pst = null;
				ResultSet rs = null;

				try {
					// StopWatch dt1 = new StopWatch();
					// dt1.start();

					svr = new SvReader(sessionId);
					svr.setIncludeGeometries(true);
					SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);
					geomArr = new ArrayList<Geometry>();

					// Should we include hasGeometry check?
					// despite not being represented by geometry data
					// i.e quarantine not declared via map
					// there may very likely be a set of holdings
					// which are linked/blacklisted.

					// Method should build geometry set of movements
					// pertaining to those holdings, even though the actual
					// input quarantine is not being drawn/rendered on the map
					Reader rdr = new Reader();

					DbDataArray moveArr = new DbDataArray();
					// concur_read_only and type_forward_only set by
					// default, just execute
					pst = svr.dbGetConn().prepareStatement(
							"SELECT * FROM " + SvConf.getDefaultSchema() + ".get_movements_by_quarantine(?)");
					pst.setInt(1, objectId.intValue());

					rs = pst.executeQuery();
					while (rs.next()) {
						DbDataObject moveDbo = new DbDataObject();
						ResultSetMetaData metaData = rs.getMetaData();
						for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
							moveDbo.setVal(metaData.getColumnName(i).toUpperCase(), rs.getObject(i));
						}

						moveArr.addDataItem(moveDbo);
					}
					geomArr = buildMovements(svr, rdr, moveArr, startDate, expiryDate);
					// dt1.split();
					// System.out.println("Query processed and geometry built in
					// dT = " + dt1.toSplitString());

					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					if (e instanceof SvException) {
						log4j.error(((SvException) e).getFormattedMessage(), e);
					} else {
						log4j.error(e);
					}

				} finally {
					try {
						SvCore.closeResource((AutoCloseable) pst, svr.getInstanceUser());
						SvCore.closeResource((AutoCloseable) rs, svr.getInstanceUser());
					} catch (Exception e) {
						String errMsg = "Cannot close resource in blacklisted movements assembly";
						if (e instanceof SvException)
							errMsg = ((SvException) e).getJsonMessage();
						try {
							stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
						} catch (IOException ioe) {
							log4j.error("Stream closed, can't write error", ioe);
						}
						log4j.error(errMsg, e);
					}

					if (svr != null)
						svr.release();
				}
			}
		};
	}

	/**
	 * 
	 * @param sessionId
	 * @param startDate
	 * @param endDate
	 * @param includeHistory
	 * @return stream of FeatureCollection<Geometry>
	 */
	@GET
	@Path("/getQuarantines/bbox/{sessionId}/{startDate}/{endDate}/{filterHistory}")
	@Produces("application/pbf")
	public StreamingOutput getQuarantines(@PathParam("sessionId") final String sessionId,
			@PathParam("startDate") final String startDate, @PathParam("endDate") final String endDate,
			@PathParam("filterHistory") String filterHistory) {

		return new StreamingOutput() {
			public void write(OutputStream stream) {

				SvReader svr = null;
				try {
					svr = new SvReader(sessionId);
					svr.setIncludeGeometries(true);
					SvGeobufEncoder enc = new SvGeobufEncoder(stream, 10);

					ArrayList<Geometry> geomArr = new ArrayList<Geometry>();
					DbDataArray dbArr = new DbDataArray();
					ValidationChecks vc = new ValidationChecks();

					dbArr = svr.getObjectsByTypeId(SvCore.getTypeIdByName(Tc.QUARANTINE_GEOMETRY), null, null, null);
					for (DbDataObject dbo : dbArr.getItems()) {
						DbDataObject parentDbo = svr.getObjectById(dbo.getParent_id(),
								SvCore.getTypeIdByName(Tc.QUARANTINE), null);

						if (parentDbo != null) {
							Geometry geom = extendMetadata(SvGeometry.getGeometry(dbo), parentDbo);

							if (checkQuarantineDateFilter(parentDbo, startDate, endDate)) {
								if (vc.checkIfQuarantineActive(parentDbo)) {
									setDescriptor(geom, Tc.QUARANTINE_ACTIVE);
									geomArr.add(geom);
								} else {
									if (!Boolean.valueOf(filterHistory)) {
										setDescriptor(geom, Tc.QUARANTINE_INACTIVE);
										geomArr.add(geom);
									}
								}
							}
						}
					}

					enc.writeSvGeometry(geomArr);
				} catch (Exception e) {
					String errMsg = "Failed fetching geometry set. Please see server logs";
					if (e instanceof SvException)
						errMsg = ((SvException) e).getJsonMessage();
					try {
						stream.write(errMsg.getBytes(StandardCharsets.UTF_8));
					} catch (IOException ioe) {
						log4j.error("Stream closed, can't write error", ioe);
					}
					log4j.error(errMsg, e);
				} finally {
					if (svr != null) {
						svr.release();
					}
				}
			};
		};
	}

	@GET
	@Path("/getMapOrigin/holding/{token}/{objId}/{objType}")
	@Produces("text/html;charset=utf-8")
	public Response getHoldingLocation(@PathParam("token") String token, @PathParam("objId") Long objId,
			@PathParam("objType") Long objType) {

		String cnt = "";
		SvReader svr = null;

		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);

			Point point = getHoldingGeometry(svr, svr.getObjectById(objId, objType, null));
			if (point != null)
				cnt = stringifyPoint(point);

		} catch (SvException e) {
			log4j.error("Failed fetching object centroid", e);
		} finally {
			if (svr != null)
				svr.release();
		}

		return Response.status(200).entity(cnt).build();
	};

	@GET
	@Path("/getMapOrigin/animal/{token}/{parentId}")
	@Produces("text/html;charset=utf-8")
	public Response getAnimalLocation(@PathParam("token") String token, @PathParam("parentId") Long parentId) {

		String cnt = "";
		SvReader svr = null;

		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);

			Point point = getHoldingGeometry(svr,
					svr.getObjectById(parentId, SvCore.getTypeIdByName(Tc.HOLDING), null));
			if (point != null)
				cnt = stringifyPoint(point);

		} catch (SvException e) {
			log4j.error("Failed fetching object centroid", e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(cnt).build();
	};

	@GET
	@Path("/getMapOrigin/pet/{token}/{petObjId}")
	@Produces("text/html;charset=utf-8")
	public Response getPetLocation(@PathParam("token") String token, @PathParam("petObjId") Long petObjId) {

		String cnt = "";
		SvReader svr = null;

		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);
			DbDataArray allPetLocations = svr.getObjectsByParentId(petObjId,
					SvReader.getTypeIdByName("STRAY_PET_LOCATION"), null, 0, 0);
			if (!allPetLocations.getItems().isEmpty()) {
				Point point = null;
				ArrayList<DbDataObject> tempAllPetLocations = allPetLocations.getSortedItems("DT_INSERT");

				for (int i = tempAllPetLocations.size() - 1; i >= 0; i--) {
					point = getPetGeometry(svr, svr.getObjectById(tempAllPetLocations.get(i).getObject_id(),
							SvCore.getTypeIdByName(Tc.STRAY_PET_LOCATION), null));

					if (point != null)
						break;
				}

				if (point != null)
					cnt = stringifyPoint(point);
			}

		} catch (SvException e) {
			log4j.error("Failed fetching object centroid", e);
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(cnt).build();
	};

	@GET
	@Path("/getMapOrigin/quarantine/{token}/{objId}/{objType}")
	@Produces("text/html;charset=utf-8")
	public Response getQuarantineLocation(@PathParam("token") String token, @PathParam("objId") Long objId,
			@PathParam("objType") Long objType) {

		String bbox = "";
		SvReader svr = null;

		try {
			svr = new SvReader(token);
			svr.setIncludeGeometries(true);

			DbDataArray qGeom = svr.getObjectsByParentId(objId, SvCore.getTypeIdByName(Tc.QUARANTINE_GEOMETRY), null,
					null, null);

			if (!qGeom.getItems().isEmpty()) {
				Envelope env = SvGeometry.getGeometry(qGeom.get(0)).getEnvelopeInternal();
				bbox = SvGeometry.getBBox(env);
			}
		} catch (SvException e) {
			log4j.error("Failed fetching object centroid", e);
		} finally {
			if (svr != null)
				svr.release();
		}

		return Response.status(200).entity(bbox).build();
	};

	@GET
	@Path("/getSearchLocations/{token}/{searchStr}")
	@Produces("application/json")
	public Response getSearchLocations(@PathParam("token") String token, @PathParam("searchStr") String searchStr)
			throws SvException {

		SvReader svr = null;
		JsonArray jArr = null;
		JsonObject config = new Gson().fromJson(
				"{\"HOLDING\": {\"CRITERIA\": [\"PIC\", \"NAME\", \"VILLAGE_CODE\"], \"MARKUP\": [\"PIC\", \"NAME\"]},\"ANIMAL\": {\"CRITERIA\": [\"ANIMAL_ID\"], \"MARKUP\": [\"ANIMAL_ID\"]},\"QUARANTINE\": {\"CRITERIA\": [\"REASON\"], \"MARKUP\": [\"REASON\"]},\"SVAROG_SDI_UNITS\": {\"CRITERIA\": [\"UNIT_NAME\"], \"MARKUP\": [\"UNIT_NAME\"]}}",
				JsonObject.class);
		try {
			svr = new SvReader(token);
			jArr = new SearchLocations(svr, searchStr, config).fetch(10).build();
			if (jArr == null) {
				jArr = new JsonArray();
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error(((SvException) e).getFormattedMessage(), e);
			} else {
				log4j.error(e);
			}
		} finally {
			if (svr != null)
				svr.release();
		}

		return Response.status(200).entity(jArr.toString()).build();
	};
}
