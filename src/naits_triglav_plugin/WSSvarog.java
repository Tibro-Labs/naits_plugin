/*******************************************************************************
 * Copyright (c),  2017 TIBRO DOOEL Skopje
 *******************************************************************************/

package naits_triglav_plugin;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.prtech.svarog.CodeList;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvFileStore;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

/**
 * implementation of SVAROG platform functionalities
 * 
 * @author TIBRO_001
 *
 */

@Path("/svarog")
public class WSSvarog {

	LinkedHashMap<String, LinkedHashMap<String, Object>> linkVals = new LinkedHashMap<>();
	static final Logger log4j = LogManager.getLogger(WSSvarog.class.getName());

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RANGE));
		return true;
	}

	@Path("/getObjectsById/{session_id}/{object_id}/{type_id}")
	@GET
	@Produces("application/json")
	public Response getObjectById(@PathParam("session_id") String sessionId, @PathParam("object_id") Long id,
			@PathParam("type_id") Long typeId) throws JSONException {
		SvReader db = null;
		String retvalString1 = "";
		try {
			db = new SvReader(sessionId);

			JsonObject jSobj2 = null;
			long[] retval = new long[1];
			DbDataObject object = db.getObjectById(id, typeId, null);

			if (retval[0] == svCONST.SUCCESS && object != null) {

				jSobj2 = object.toJson();
			}
			Gson gson = new Gson();
			if (jSobj2 != null) {
				JsonObject getObject = jSobj2.getAsJsonObject("com.prtech.svarog_common.DbDataObject");
				retvalString1 = gson.toJson(getObject);
			}
			// String retvalString = retvalString1.
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			retvalString1 = e.getFormattedMessage();
		} finally {
			if (db != null)
				db.release();
		}
		return Response.status(200).entity(retvalString1).build();
	}

	@Path("/i18n/{locale}/{label_group}")
	@GET
	@Produces("application/json")
	public Response getI18NLabels(@PathParam("label_group") String labelsGroup, @PathParam("locale") String locale)
			throws JSONException {

		DbDataArray dbaLablels;
		JsonObject jsLabels = new JsonObject();
		try {
			dbaLablels = I18n.getLabels(locale, labelsGroup);
			if (labelsGroup != null && labelsGroup.length() > 3) {
				String lblCode = null;
				for (DbDataObject dbLabel : dbaLablels.getItems()) {
					lblCode = (String) dbLabel.getVal("label_code");
					if (lblCode != null && lblCode.startsWith(labelsGroup)) {
						jsLabels.addProperty(lblCode, (String) dbLabel.getVal("LABEL_TEXT"));
					}
				}
			}
		} catch (SvException e) {
			log4j.error("Error in WsSvarog.getI18NLabels:" + e.getFormattedMessage(), e);
		}
		return Response.status(200).entity(jsLabels.toString()).build();
	}

	@Path("/deleteObject/{session_id}/{type_id}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response deleteObject(@Context HttpServletRequest httpRequest, @PathParam("session_id") String sessionId,
			@PathParam("type_id") Long typeId, MultivaluedMap<String, String> formVals) throws JSONException {
		SvReader db = null;
		SvWriter svw = null;
		try {
			db = new SvReader(sessionId);
			svw = new SvWriter(db);
			db.setAutoCommit(false);
			Object obj = null;
			// System.out.println(formVals.toString());
			if (formVals.get("id") != null) {
				List<String> getVal = formVals.get("id");
				obj = getVal.iterator().next();
				Long object_Id = Long.parseLong(obj.toString());
				Gson gson = new Gson();
				DbDataObject object = db.getObjectById(object_Id, typeId, null);
				String result = gson.toJson(object.toJson());

				JsonObject jSobj2 = null;

				jSobj2 = gson.fromJson(result, JsonObject.class);
				DbDataObject db_obj = new DbDataObject();
				db_obj.fromJson(jSobj2);
				svw.deleteObject(db_obj);
				db.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
		}
		return Response.status(200).entity("success").build();
	}

	@Path("/deleteObjectWithSaveCheck/{session_id}/{type_id}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response deleteObjectWithSaveCheck(@Context HttpServletRequest httpRequest,
			@PathParam("session_id") String sessionId, @PathParam("type_id") Long typeId,
			MultivaluedMap<String, String> formVals) throws JSONException {
		SvReader db = null;
		SvWriter svw = null;
		try {
			db = new SvReader(sessionId);
			svw = new SvWriter(db);
			db.setAutoCommit(false);
			Object obj = null;
			// System.out.println(formVals.toString());
			if (formVals.get("id") != null) {
				List<String> getVal = formVals.get("id");
				obj = getVal.iterator().next();
				Long object_Id = Long.parseLong(obj.toString());
				Gson gson = new Gson();
				DbDataObject object = db.getObjectById(object_Id, typeId, null);
				String result = gson.toJson(object.toJson());

				JsonObject jSobj2 = null;

				jSobj2 = gson.fromJson(result, JsonObject.class);
				DbDataObject db_obj = new DbDataObject();
				db_obj.fromJson(jSobj2);
				db_obj.setDt_delete(new DateTime());
				svw.saveObject(db_obj);
				db.dbCommit();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
		}
		return Response.status(200).entity("success").build();
	}

	/**
	 * Web service version of SvWriter.deleteObject delete the object with given
	 * ID, type and PKid (version) if there is greater PKid (same object changed
	 * by other user) object should not be deleted
	 * 
	 * @param sessionId
	 *            Session ID (SID) of the web communication between browser and
	 *            web server
	 * @param objectId
	 *            ID of the object that we want to delete
	 * @param objectType
	 *            type of the Object that we try to delete
	 * @param objectPkId
	 *            PkId (version) of the object that we have
	 * 
	 * @return true if object was deleted with no errors
	 */
	@Path("/deleteObjectWithSaveCheck/{sessionId}/{objectId}/{objectType}/{objectPkId}")
	@POST
	@Produces("application/json")
	public Response deleteObjectWithSaveCheck(@PathParam("sessionId") String sessionId,
			@PathParam("objectId") Long objectId, @PathParam("objectType") Long objectType,
			@PathParam("objectPkId") Long objectPkId, @Context HttpServletRequest httpRequest) {
		String returnString = "false";
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dbObj = svr.getObjectById(objectId, objectType, null);
			if (dbObj != null) {
				dbObj.setDt_delete(new DateTime());
				svw.saveObject(dbObj);
				svw.deleteObject(dbObj);
				returnString = "true";
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			return Response.status(401).entity(e.getFormattedMessage()).build();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(returnString).build();
	}

	@Path("/saveObject/{session_id}/{type_id}/{parent_id}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveObject(@Context HttpServletRequest httpRequest, @PathParam("session_id") String sessionId,
			@PathParam("type_id") Long typeId, @PathParam("parent_id") Long parent_id,
			MultivaluedMap<String, String> formVals) throws JSONException {
		// JsonObject jSobj2 = null;
		SvReader db = null;
		SvWriter svw = null;
		DbDataObject object_toSave = null;
		try {
			db = new SvReader(sessionId);
			db.setAutoCommit(false);
			svw = new SvWriter(db);
			String measureValue = "";
			// System.out.println(formVals.toString());

			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey().equals("ROOT_PARENT_ID")) {
					// System.out.println(entry.getValue());

				}
				if (entry.getKey().equals("VALUE_ID")) {
					if (entry.getValue() != null && !entry.getValue().isEmpty()) {
						List<String> tvl = entry.getValue();
						measureValue = tvl.iterator().next();
					}
				}
			}

			object_toSave = new DbDataObject();
			if (parent_id != null && parent_id != 0) {
				object_toSave.setParent_id(parent_id);
			}
			if (parent_id == null) {
				parent_id = (long) 0;
			}
			object_toSave.setObject_type(typeId);
			DbDataArray fields = db.getObjectsByParentId(typeId, svCONST.OBJECT_TYPE_FIELD, null, 0, 0);
			for (DbDataObject dbo : fields.getItems()) {
				String fName = (String) dbo.getVal("FIELD_NAME");
				String fType = (String) dbo.getVal("FIELD_TYPE");
				// String fGUIMeta = (String) dbo.getVal("GUI_METADATA");

				try {
					Object obj = null;

					if (formVals.get(fName) != null) {
						if (fType.equals("NVARCHAR")) {
							List<String> formVal = formVals.get(fName);
							obj = formVal.iterator().next();
							if (fName.equals("PARENT_ID")) {
								// String s = obj.toString();
								// System.out.println(s);
							}

						}

						if (fType.equals("NUMERIC")) {
							List<String> tvl = formVals.get(fName);
						}
					}

					if (fType.equals("TIMESTAMP") || fType.equals("TIME") || fType.equals("DATE")) {
						List<String> tvl1 = formVals.get(fName);
						String val = tvl1.iterator().next();

						String dtPattern = (fType.equals("TIMESTAMP") || fType.equals("DATE")
								? SvConf.getDefaultDateFormat() : "");

						DateTime final_date = DateTime.parse(val, DateTimeFormat.forPattern(dtPattern));
						object_toSave.setVal(fName, final_date);
						continue;
					}
					object_toSave.setVal(fName, obj);
				} catch (Exception ex) {

				}
			}

			svw.saveObject(object_toSave);
			svw.dbCommit();
		} catch (Exception e) {
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
			return Response.status(401).entity(e.getMessage()).build();
		} finally {
			if (db != null)
				db.release();
			if (svw != null)
				svw.release();
		}

		return Response.status(200).entity("ok").type("text/plain").build();
	}

	/**
	 * Method to publish the result of Svarog's DbUtil.getObjectsByParentId
	 * 
	 * @param sessionId
	 *            The current token of the user session
	 * @param parent_id
	 *            The ID of the parent object
	 * @param tableName
	 *            The ID of the type of object which should be returned
	 * @param page
	 *            The page number to be returned
	 * @param row_per_page
	 *            How many rows should be displayed on a single page
	 * @param sortByField
	 *            Name of the field according to which the list should be sorted
	 * @return
	 */
	private Response getObjectsByParentImpl(String sessionId, Long parent_id, String tableName, Integer page,
			Integer row_per_page, String sortByField) {
		SvReader db = null;
		CodeList cl = null;
		String retvalString = "";
		try {
			db = new SvReader(sessionId);
			cl = new CodeList(db);

			page = page < 1 ? 1 : page;

			int vRowLimit = 0;
			int vOffset = 0;
			JsonObject jsObj = null;

			vOffset = (page - 1) * row_per_page;
			vRowLimit = page * row_per_page;

			Long tableObjId = SvReader.getTypeIdByName(tableName);

			DbDataArray fields = db.getObjectsByParentId(tableObjId, svCONST.OBJECT_TYPE_FIELD_SORT, null, 0, 0);
			DbDataArray repoFields = db.getObjectsByParentId(svCONST.OBJECT_TYPE_REPO, svCONST.OBJECT_TYPE_FIELD_SORT,
					null, 0, 0);

			DbDataArray dbArray = db.getObjectsByParentId(parent_id, tableObjId, null, vRowLimit, vOffset);

			if (dbArray != null) {

				JsonObject exParams = new JsonObject();
				exParams.addProperty("dt_dateformat", SvConf.getDefaultDateFormat());
				exParams.addProperty("dt_timeformat", SvConf.getDefaultTimeFormat());
				jsObj = dbArray.getMembersJson().getTabularJson("", dbArray, repoFields, fields, exParams, cl);
			}

			Gson gson = new Gson();
			retvalString = gson.toJson(jsObj);
		} catch (Exception e) {
			if (db != null)
				db.release();
			if (cl != null)
				cl.release();
			return Response.status(401).entity(e.getMessage()).build();
		} finally {
			if (db != null)
				db.release();
			if (cl != null)
				cl.release();
		}
		return Response.status(200).entity(retvalString).build();

	}

	/**
	 * Method to publish the result of Svarog's DbUtil.getObjectsByParentId
	 * 
	 * @param sessionId
	 *            The current token of the user session
	 * @param parent_id
	 *            The ID of the parent object
	 * @param typeId
	 *            The ID of the type of object which should be returned
	 * @param page
	 *            The page number to be returned
	 * @param row_per_page
	 *            How many rows should be displayed on a single page
	 * @param httpRequest
	 *            Reference to the HttpRequest object
	 * @param sortByField
	 *            Name of the field according to which the list should be sorted
	 * @return
	 */
	@Path("/getObjectsByParentId/{session_id}/{tableName}/{parent_id}/{page}/{row_per_page}/{sort_by_field}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("session_id") String sessionId,
			@PathParam("parent_id") Long parent_id, @PathParam("tableName") String tableName,
			@PathParam("page") Integer page, @PathParam("row_per_page") Integer row_per_page,
			@PathParam("sort_by_field") String sortByField) {

		return getObjectsByParentImpl(sessionId, parent_id, tableName, page, row_per_page, sortByField);
	}

	/**
	 * Same as the overloaded method, but without sort order
	 * 
	 * @param sessionId
	 *            The current token of the user session
	 * @param parent_id
	 *            The ID of the parent object
	 * @param typeId
	 *            The ID of the type of object which should be returned
	 * @param page
	 *            The page number to be returned
	 * @param row_per_page
	 *            How many rows should be displayed on a single page
	 * @param httpRequest
	 *            Reference to the HttpRequest object
	 * @param sortByField
	 *            Name of the field according to which the list should be sorted
	 * @return
	 */
	@Path("/getObjectsByParentId/{session_id}/{tableName}/{parent_id}/{page}/{row_per_page}")
	@GET
	@Produces("application/json")
	public Response getObjectsByParentId(@PathParam("session_id") String sessionId,
			@PathParam("parent_id") Long parent_id, @PathParam("tableName") String tableName,
			@PathParam("page") Integer page, @PathParam("row_per_page") Integer row_per_page) {

		return getObjectsByParentImpl(sessionId, parent_id, tableName, page, row_per_page, null);
	}

	@Path("/getObjectsByTypeId/{session_id}/{type_id}/{page}/{row_per_page}/{object_id}")
	@GET
	@Produces("application/json")
	public Response getObjectsByTypeId(@PathParam("session_id") String sessionId, @PathParam("type_id") Long typeId,
			@PathParam("page") Integer page, @PathParam("row_per_page") Integer row_per_page,
			@PathParam("object_id") Long object_id, @Context HttpServletRequest httpRequest) {
		SvReader db = null;
		String retvalString = "";
		try {
			db = new SvReader(sessionId);

			page = page < 1 ? 1 : page;

			Integer vRowLimit = 0;
			Integer vOffset = 0;
			JsonObject jsObj = null;

			long[] retval = new long[1];
			vOffset = (page - 1) * row_per_page;
			vRowLimit = page * row_per_page;

			DbDataObject table = db.getObjectById(typeId, svCONST.OBJECT_TYPE_TABLE, null);
			if (retval[0] != svCONST.SUCCESS || table == null)
				return Response.status(500).build();

			DbDataArray fields = db.getObjectsByParentId(typeId, svCONST.OBJECT_TYPE_FIELD_SORT, null, 0, 0);
			DbDataArray repoFields = db.getObjectsByParentId(svCONST.OBJECT_TYPE_REPO, svCONST.OBJECT_TYPE_FIELD_SORT,
					null, 0, 0);
			DbDataArray dbArray = db.getObjects(null, typeId, null, vRowLimit, vOffset);

			if (retval[0] == svCONST.SUCCESS && dbArray != null) {

				JsonObject exParams = new JsonObject();
				exParams.addProperty("dt_dateformat", SvConf.getDefaultDateFormat());
				exParams.addProperty("dt_timeformat", SvConf.getDefaultTimeFormat());
				jsObj = dbArray.getMembersJson().getTabularJson("", dbArray, repoFields, fields, exParams, null);
			}

			Gson gson = new Gson();
			retvalString = gson.toJson(jsObj);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			retvalString = e.getFormattedMessage();
		} finally {
			if (db != null)
				db.release();
		}
		return Response.status(200).entity(retvalString).build();
	}

	@Path("getObjectsByDbSearch/{session_id}/{type_id}/{dbSearchString}/{dbSearchValue}")
	@GET
	@Produces("application/json")
	public Response getObjectsByDbSearch(@PathParam("session_id") String sessionId, @PathParam("type_id") Long type_id,
			@PathParam("dbSearchString") String dbSearchString, @PathParam("dbSearchValue") String dbSearchValue,
			@PathParam("rowLimit") Integer rowLimit, @PathParam("offset") Integer offset) {

		SvReader db = null;
		String retvalString = null;
		try {
			db = new SvReader(sessionId);

			int vRowLimit = 0;
			int vOffset = 0;
			JsonObject jSobj2 = null;
			long[] retval = new long[1];

			DbSearchExpression dbSearchExpressionT = new DbSearchExpression();
			DbSearchCriterion dbsearch = new DbSearchCriterion(dbSearchString, DbCompareOperand.LIKE, dbSearchValue);
			dbSearchExpressionT.addDbSearchItem(dbsearch);

			if (rowLimit != null && rowLimit > 0)
				vRowLimit = rowLimit;
			else
				vRowLimit = 500;
			if (offset != null && offset > 0)
				vOffset = offset;
			else
				vOffset = 0;

			DbDataArray dbArray = db.getObjects(dbSearchExpressionT, type_id, null, vRowLimit, vOffset);

			if (retval[0] == svCONST.SUCCESS && dbArray != null) {
				jSobj2 = dbArray.getMembersJson().getMembersToJson("", dbArray, null);
			}
			Gson gson = new Gson();
			if (jSobj2 != null && jSobj2.isJsonNull() == false) {
				JsonObject obj = jSobj2.getAsJsonObject("com.prtech.svarog_common.DbDataObject");
				retvalString = gson.toJson(obj);
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			retvalString = e.getFormattedMessage();
		} finally {
			if (db != null)
				db.release();
		}
		return Response.status(200).entity(retvalString).build();
	}

	public static String formatDate(Date day) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yy");
		String date = formatter.format(day);
		return date;
	}

	@Path("getFiles/{session_id}/{object_id}/{file_name}/")
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getFiles(@PathParam("object_id") Long object_id, @PathParam("session_id") String session_id,
			@PathParam("file_name") String file_name) {
		byte[] bt = null;
		javax.ws.rs.core.Response.ResponseBuilder response = null;
		SvReader svr = null;
		SvFileStore fs = null;
		try {
			svr = new SvReader(session_id);
			fs = new SvFileStore(svr);
			DbDataObject DboFile = svr.getObjectById(object_id, svCONST.OBJECT_TYPE_FILE, null);
			bt = fs.getFileAsByte(DboFile);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			if (svr != null)
				svr.release();
			if (fs != null)
				fs.release();
			return Response.status(401).entity(e.getFormattedMessage()).type("text/plain").build();
		} finally {
			if (svr != null)
				svr.release();
			if (fs != null)
				fs.release();
		}
		response = Response.ok((Object) bt);
		response.header("Content-Disposition", "attachment; filename=\"" + file_name + "\"");
		return response.build();
	}

	@Path("/getFileNames/{session_id}/{object_id}/{type_id}/")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getFileNames(@PathParam("session_id") String session_id, @PathParam("object_id") Long objectId,
			@PathParam("type_id") Long typeId) {
		String file_Name = "";
		BigDecimal file_id;
		Gson gson = new Gson();
		JsonObject jso = new JsonObject();
		SvFileStore fs = null;
		String retvalString = "";
		try {
			fs = new SvFileStore(session_id);
			DbDataArray object = fs.getFiles(objectId, typeId, "", null);
			for (DbDataObject dbo : object.getItems()) {
				file_Name = (String) dbo.getVal("FILE_NAME");
				file_id = (BigDecimal) dbo.getVal("FILE_ID");
				jso.addProperty(file_id.toString(), file_Name);
			}
			retvalString = gson.toJson(jso);
			// System.out.println(retvalString);
			int i = retvalString.length();
			// sSystem.out.println(i);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
			retvalString = e.getFormattedMessage();
		} finally {
			if (fs != null)
				fs.release();
		}
		return Response.status(200).entity(retvalString).build();
	}

	/**
	 * procedure to add new value in DbDataObject from JsonObject
	 * 
	 * @param vdataObject
	 *            DbDataObject in which we want to save the new value
	 * @param fieldName
	 *            String name of the field, it has to be same value in
	 *            JsonObject and in DbDataObject
	 * @param vfieldObject
	 *            DbDataObject we can read the type of the object and scale from
	 *            this: NUMERIC, NVARCHAR, BOOLEAN, DATE, DATETIME, TIMESTAMP,
	 * @param jsonData
	 *            JsonObject type of the field that we want to read/save:
	 *            string, bool, number, date
	 * 
	 * @return DbDataObject with new field value added
	 */
	public DbDataObject addValueToDataObject(DbDataObject vdataObject, String fieldName, DbDataObject vfieldObject,
			JsonObject jsonData) {
		String fieldType = vfieldObject.getVal("FIELD_TYPE").toString();
		Long scale = (Long) vfieldObject.getVal("FIELD_SCALE");
		Gson gson = new Gson();
		JsonObject jsonObjRet = jsonData;
		// if object is inside group path, get it out
		JsonObject guiMetadata = null;
		try {
			if (vfieldObject.getVal("GUI_METADATA") != null)
				guiMetadata = gson.fromJson(vfieldObject.getVal("GUI_METADATA").toString(), JsonObject.class);
		} catch (Exception e) {
			log4j.error(e);
		}

		if (guiMetadata != null && guiMetadata.has("react")) {
			JsonObject jsonreactGUI = (JsonObject) guiMetadata.get("react");
			if (jsonreactGUI != null && jsonreactGUI.has("grouppath"))
				jsonObjRet = (JsonObject) jsonData.get(jsonreactGUI.get("grouppath").getAsString());
		}
		// V2.1 fix if field is null 29.09.2017
		if (jsonObjRet != null) {

			if (jsonObjRet.has(fieldName)) {// V 2.1 fix for null values
				switch (fieldType) {
				case "NUMERIC":
					if (scale == null || scale <= 0) {
						Long tmpLong = jsonObjRet.get(fieldName).getAsLong();
						vdataObject.setVal(fieldName, tmpLong);
					} else {
						Double tmpDouble = jsonObjRet.get(fieldName).getAsDouble();
						vdataObject.setVal(fieldName, tmpDouble);
					}
					break;
				case "NVARCHAR":
					vdataObject.setVal(fieldName, jsonObjRet.get(fieldName).getAsString());
					break;
				case "BOOLEAN":
					vdataObject.setVal(fieldName, jsonObjRet.get(fieldName).getAsBoolean());
					break;
				case "DATE":
					DateTime date = new DateTime(jsonObjRet.get(fieldName).getAsString());
					vdataObject.setVal(fieldName, date);
					break;
				case "TIMESTAMP":
				case "DATETIME":
					DateTime pickDate = new DateTime(jsonObjRet.get(fieldName).getAsString());
					vdataObject.setVal(fieldName, pickDate);
					break;
				default:
				}
			} else
				vdataObject.setVal(fieldName, null);
		}
		return vdataObject;
	}

	/**
	 * procedure to check the name of the field, we are not able to return or
	 * process fields : PKID, since this is the connection to SVAROG table,
	 * GUI_METADATA it has lots of JSON in it and configurations, CENTROID and
	 * GEOM are complex data-type and can't be displayed as any other similar
	 * format
	 * 
	 * @param fieldName
	 *            String field name that we like to process
	 * 
	 * @return true if the field is "normal" and can be processed, false if its
	 *         complex or funny field
	 */
	public static Boolean processField(String fieldName) {
		if ("PKID".equalsIgnoreCase(fieldName) || "GUI_METADATA".equalsIgnoreCase(fieldName)
				|| "CENTROID".equalsIgnoreCase(fieldName) || "GEOM".equalsIgnoreCase(fieldName))
			return false;

		return true;
	}

	public DbDataObject generateQuarantineDboFromMapData(SvReader svr, Long objectId,
			MultivaluedMap<String, String> formVals, String radius, boolean isUpdate) throws SvException {
		DbDataObject qDbo = new DbDataObject();
		DbDataArray qFields = SvCore.getFields(SvCore.getTypeIdByName("QUARANTINE"));
		Long qType = SvCore.getTypeIdByName("QUARANTINE");

		String formData = "";
		JsonObject data = null;
		Gson gson = new Gson();
		if (isUpdate) {
			qDbo = svr.getObjectById(objectId, qType, null);
		} else {
			qDbo.setObject_id(0L);
			qDbo.setObject_type(qType);
		}

		formData = formData.replace(",P!_1-", "/");
		formData = formData.replace(",P!_2-", "\\");
		// handle empty, prep json, create data
		for (Entry<String, List<String>> entry : formVals.entrySet()) {
			if (entry.getKey() != null && !entry.getKey().isEmpty()) {
				String key = entry.getKey();
				formData = key;
			}
		}
		data = gson.fromJson(formData, JsonObject.class);

		if (data != null && data.has("status"))
			qDbo.setStatus(data.get("status").getAsString());

		// Map form values to dbo fields
		for (DbDataObject field : qFields.getItems()) {
			String fName = field.getVal("FIELD_NAME").toString();
			if (WSSvarog.processField(fName) && !"".equalsIgnoreCase(field.getVal("FIELD_TYPE").toString())) {
				qDbo = addValueToDataObject(qDbo, fName, field, data);
			}
		}
		// set radius, hidden field, _mRadius drawn object property, value in
		// meters
		if (radius != null)
			qDbo.setVal(Tc.RADIUS, Long.valueOf(radius));
		// TODO case when centar of quarantine is not null

		// hardcode quarantine type to blacklist, regardless of user choice
		qDbo.setVal(Tc.QUARANTINE_TYPE, "1");

		// format date
		DateTime start = new DateTime(qDbo.getVal(Tc.DATE_FROM).toString());
		DateTime end = new DateTime(qDbo.getVal(Tc.DATE_TO).toString());
		java.sql.Date dd = new java.sql.Date(start.getMillis());
		java.sql.Date ed = new java.sql.Date(end.getMillis());

		qDbo.setVal(Tc.DATE_FROM, dd);
		qDbo.setVal(Tc.DATE_TO, ed);

		return qDbo;
	}
}