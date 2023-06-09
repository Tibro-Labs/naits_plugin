package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;
import com.prtech.svarog_common.SvCharId;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.geojson.GeoJsonWriter;

public class GeoCoder {

	static final Logger log4j = LogManager.getLogger(WsGeometry.class.getName());

	/**
	 * Svarog reader instance<br>
	 * Be responsible, close in service when done
	 * 
	 * @svr
	 */
	protected SvReader svr;

	/**
	 * Writer for serialization of wkt geometries (jts standard) to GeoJSON
	 * objects
	 */
	private GeoJsonWriter wkt2Json = new GeoJsonWriter();

	/**
	 * Geocoder configuration object, valid json
	 * 
	 */
	private JsonObject config;

	/**
	 * Query results as list of svarog dbdata objects
	 */
	ArrayList<DbDataObject> items = new ArrayList<DbDataObject>();

	/**
	 * The search string inputted by the user
	 */
	private String input;

	/**
	 * Array of custom JsonObjects built in this.Result.serialize() <br>
	 * <br>
	 */
	private JsonArray output = new JsonArray();

	/**
	 * 
	 *
	 *
	 */
	public class Result {
		// init fields, map dbo, convenience
		// shadow enclosing class config field, use own type-specific config
		private Long id;
		private String type;
		private LinkedHashMap<SvCharId, Object> values;
		private JsonObject config;
		// props
		private String markup;
		private Geometry geometry;

		private Result(DbDataObject dbo) throws SvException {
			this.id = dbo.getObject_id();
			this.type = (String) SvCore.getDbt(dbo).getVal("TABLE_NAME");
			this.values = dbo.getValuesMap();
			this.config = GeoCoder.this.config.get(this.type).getAsJsonObject();
			this.markup = setMarkup(this.type, this.config.get("MARKUP").getAsJsonArray(), this.values);
			this.geometry = setGeometry(this.type, dbo);

			if (this.geometry == null)
				throw new SvException(
						"Failed search result initialization." + " Object does not have geometry. "
								+ " Check your setGeometry implementation."
								+ " While geometry is null, result will be ommited from the set.",
						svr.getInstanceUser());
		}

		/**
		 * Serialize result to JsonObject <br>
		 * <br>
		 * 
		 * Serialization of java beans does not work for inner classes, it may
		 * serialize the outer class members. Approach voided. <br>
		 * Geometry (wkt format, jts standard) is serialized to GeoJSON.
		 * 
		 * @return
		 */
		public JsonObject serialize() {
			JsonObject jo = new JsonObject();
			jo.addProperty("id", this.id);
			jo.addProperty("type", this.type);
			if (this.values != null)
				// check LinkedHashMap to string
				// this actually works :D
				jo.addProperty("values", this.values.toString());
			if (this.markup != null)
				jo.addProperty("markup", this.markup);
			if (this.geometry != null)
				jo.addProperty("geometry", wkt2Json.write(this.geometry));

			return jo;
		}
	}

	/**
	 * Alt constructor, optional configuration object omitted. <br>
	 * <br>
	 * Advised to build your own configuration object and not use this.<br>
	 * Defaults to sys sdi_units search, by name.
	 * 
	 * @param svr
	 * @param input
	 */
	public GeoCoder(SvReader svr, String input) {
		this(svr, input,
				new Gson().fromJson(
						"{\"SVAROG_SDI_UNITS\": {\"CRITERIA\": [\"UNIT_NAME\"], \"MARKUP\": [\"UNIT_NAME\"]}}",
						JsonObject.class));
	}

	/**
	 * Default constructor <br>
	 * <br>
	 * 
	 * config_structure: <br>
	 * {dbtName: {criteria: [...crit1], markup: [...markup1]}}
	 * 
	 * @param svr
	 * @param input
	 * @param config
	 */
	public GeoCoder(SvReader svr, String input, JsonObject config) {
		this.svr = svr;
		this.input = input;
		this.config = config;
	}

	/**
	 * 
	 * @param type
	 * @param searchStr
	 * @param critArr
	 * @return DbSearchExpression
	 * @throws SvException
	 */
	protected DbSearchExpression genSearchEx(String type, String searchStr, JsonArray critArr) throws SvException {
		DbSearchExpression dbse = new DbSearchExpression();
		if (searchStr != null && critArr.size() > 0) {
			for (int i = 0; i < critArr.size(); i++) {
				String name = critArr.get(i).getAsString();
				DbSearchCriterion crit = new DbSearchCriterion(name, DbCompareOperand.ILIKE, Tc.PERCENT_OPERATOR + searchStr + Tc.PERCENT_OPERATOR);
				if (i < critArr.size() - 1) {
					crit.setNextCritOperand("OR");
				}
				dbse.addDbSearchItem(crit);
			}
		}

		return dbse;
	}

	/**
	 * Default set markup implementation. <br>
	 * Sets private this.Result.markup field. <br>
	 * Setter accessed on parent for easier override
	 * 
	 * @param type
	 *            dbtName, convenience, can be used for switch in overriden
	 *            method
	 * @param config
	 *            JsonArray of field names, will search map(3rd param) for each
	 *            element
	 * @param vals
	 *            Dbo values map of the specific result instance
	 * @return Concat string of all map values whose keys are contained in the
	 *         config array
	 */
	protected String setMarkup(String type, JsonArray config, LinkedHashMap<SvCharId, Object> vals) {
		ArrayList<String> markup = new ArrayList<>();

		for (JsonElement el : config) {
			Object val = vals.get(new SvCharId(el.getAsString()));

			if (val != null)
				markup.add(val.toString());
		}

		return String.join(" ", markup);
	}

	/**
	 * Default set geometry implementation. <br>
	 * Sets private this.Result.geometry field. <br>
	 * Setter accessed on parent for easier override
	 * 
	 * @param type
	 *            dbtName, convenience, can be used for switch in overriden
	 *            method
	 * @param dbo
	 *            DbDataObject of the specific result instance
	 * @return Geometry of the dbo, per SvGeometry default getter.
	 * @throws SvException
	 */
	protected Geometry setGeometry(String type, DbDataObject dbo) throws SvException {
		return SvGeometry.getGeometry(dbo);
	}

	/**
	 * Fetches db results, based on input and configuration of this instance.
	 * <br>
	 * Omitted optional parameter query limit, defaults to 10.
	 * 
	 * @return This
	 * @throws SvException
	 */
	public GeoCoder fetch() throws SvException {
		return fetch(10);
	}

	/**
	 * By default the driver collects all the results for the query at once.
	 * This can be inconvenient for large data sets so the JDBC driver provides
	 * a means of basing a ResultSet on a database cursor and only fetching a
	 * small number of rows. A small number of rows are cached on the client
	 * side of the connection and when exhausted the next block of rows is
	 * retrieved by repositioning the cursor. <br>
	 * <br>
	 * Requirements: <br>
	 * <br>
	 * The connection to the server must be using the V3 protocol. This is the
	 * default for (and is only supported by) server versions 7.4 and later.
	 * <br>
	 * <br>
	 * The Connection must not be in autocommit mode. The backend closes cursors
	 * at the end of transactions, so in autocommit mode the backend will have
	 * closed the cursor before anything can be fetched from it. The Statement
	 * must be created with a ResultSet type of ResultSet.TYPE_FORWARD_ONLY.
	 * This is the default, so no code will need to be rewritten to take
	 * advantage of this, but it also means that you cannot scroll backwards or
	 * otherwise jump around in the ResultSet. <br>
	 * <br>
	 * The query given must be a single statement, not multiple statements
	 * strung together with semicolons.
	 * 
	 * @param limit
	 * @return This
	 * @throws SvException
	 */
	public GeoCoder fetch(int limit) throws SvException {
		this.svr.setIncludeGeometries(true);
		int itemCnt = 0;
		// testing, testing
		// StopWatch sw = new StopWatch();
		for (Entry<String, JsonElement> entry : config.entrySet()) {
			if (limit - itemCnt < 1)
				break;

			String key = entry.getKey();
			JsonObject val = (JsonObject) entry.getValue();

			// sw.start()
			ArrayList<DbDataObject> dbArr = svr
					.getObjects(genSearchEx(key, input, val.get("CRITERIA").getAsJsonArray()),
							SvCore.getTypeIdByName(key), null, limit - itemCnt, 0)
					.getItems();
			this.items.addAll(dbArr);
			itemCnt = this.items.size();
			// sw.split();
			// System.out.println(key + "query executed in dT = " +
			// sw.toSplitString());
		}

		return this;
	}

	public JsonArray build() {
		for (DbDataObject dbo : this.items) {
			try {
				this.output.add(new Result(dbo).serialize());
			} catch (Exception e) {
				// Continue iteration on failed result init
				if (e instanceof SvException) {
					log4j.error(((SvException) e).getFormattedMessage(), e);
				} else {
					log4j.error(e);
				}
			}
		}

		return this.output;
	}
}
