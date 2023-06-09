package naits_triglav_plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;

@Path("/naits_triglav_plugin/PublicServices")
public class PublicServices {
	static final Logger log4j = LogManager.getLogger(PublicServices.class.getName());

	static final boolean isInitialized = initPlugin();

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EXPORT_CERT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.DISEASE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.AREA_HEALTH));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_BOOK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_RESULTS));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.VACCINATION_EVENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.EAR_TAG_REPLC));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.INVENTORY_ITEM));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RFID_INPUT));
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_LINK);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_MESSAGE);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_NOTIFICATION);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_ORG_UNITS);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_USER);
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_GROUP);
		return true;
	}

	SvReader setUpUser(Reader rdr) throws SvException {
		SvReader svr = null;
		String publicUsername = SvConf.getParam(Tc.reporterUserParam);
		SvSecurity svs = new SvSecurity();
		((SvCore) svs).switchUser(svCONST.serviceUser);
		svr = new SvReader(svs);
		if (publicUsername == null) {
			svr.switchUser(rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, Tc.reporterUserName, svr));
		} else {
			svr.switchUser(rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, publicUsername, svr));
		}
		if (svs != null)
			svs.release();
		return svr;
	}

	void setUpLocale(String localeId) {
		if (localeId == null || (!localeId.equals(Tc.en_US) || !localeId.equals(Tc.ka_GE))) {
			localeId = Tc.en_US;
		}
	}
	// ANIMAL

	/**
	 * Web service that executes action over list of RFIDs/Ear tags/Animal IDs and
	 * returns appropriate response in Json format
	 * 
	 * @param sessionId
	 * @param actionType
	 * @param actionSubtype
	 * @param actionDate
	 * @param animalType
	 * @param animalBreed
	 * @param destinationNumber
	 * @param textEarTags
	 * @return
	 * @throws InterruptedException
	 */
	@Path("/generateRFIDResultObjects/{sessionId}/{actionType}/{actionSubtype}/{actionDate}/{animalType}/{animalBreed}/{destinationNumber}/{textEarTags}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateRFIDResultObjects(@PathParam("sessionId") String sessionId,
			@PathParam("actionType") String actionType, @PathParam("actionSubtype") String actionSubtype,
			@PathParam("actionDate") String actionDate, @PathParam("animalType") String animalType,
			@PathParam("animalBreed") String animalBreed, @PathParam("destinationNumber") String destinationNumber,
			@PathParam("textEarTags") String textEarTags) throws InterruptedException {
		String resultJsonObj = "";
		JsonObject jObj = null;
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			wr = new Writer();
			JsonObject jObjParams = wr.buildJsonObjectByRfidInputParams(actionType, actionSubtype, actionDate,
					animalType, animalBreed, destinationNumber, textEarTags, svr);
			JsonArray jArrayParams = new JsonArray();
			jArrayParams.add(jObjParams);
			jObj = wr.generateRfidStateAndRfidResult(animalType, textEarTags, jArrayParams, svr, svw);
			resultJsonObj = jObj.toString();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(resultJsonObj).build();
	}

	/**
	 * Web service that replaces ear tag on certain animal and returns appropriate
	 * response message
	 * 
	 * @param sessionId
	 * @param currentAnimalId
	 * @param animalClass
	 * @param newEarTag
	 * @param replacementDate
	 * @param reason
	 * @param note
	 * @return
	 */
	@Path("/createEarTagReplacementAndUpdateAnimalId/{sessionId}/{currentAnimalId}/{animalClass}/{newEarTag}/{replacementDate}/{reason}/{note}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response createEarTagReplacementAndUpdateAnimalId(@PathParam("sessionId") String sessionId,
			@PathParam("currentAnimalId") String currentAnimalId, @PathParam("animalClass") String animalClass,
			@PathParam("newEarTag") String newEarTag, @PathParam("replacementDate") String replacementDate,
			@PathParam("reason") String reason, @PathParam("note") String note) {
		String result = "naits.info.animalIdNotUpdated";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		Reader rdr = null;
		DbDataObject earTagReplcObject = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			rdr = new Reader();
			wr = new Writer();
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(currentAnimalId, animalClass,
					true, svr);
			if (!dboAnimal.getStatus().equals(Tc.VALID)) {
				throw (new SvException("naits.error.earTagReplacementCanBeDoneOnlyForValidAnimals", svCONST.systemUser,
						null, null));
			}
			earTagReplcObject = wr.createEarTagReplcObject(newEarTag, replacementDate, reason, note,
					dboAnimal.getObject_id(), svr);
			svw.saveObject(earTagReplcObject, false);
			if (newEarTag != null && !newEarTag.equals("")) {
				result = wr.updateAnimalEarTagByEarTagReplacement(earTagReplcObject, svr, svw);
			}
			svw.dbCommit();
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns basic animal details without user authorization in
	 * appropriate Json format
	 * 
	 * @param animalId
	 * @param animalClass
	 * @return
	 */
	@Path("/getBasicAnimalDetails/{animalId}/{animalClass}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBasicAnimalDetails(@PathParam("animalId") String animalId,
			@PathParam("animalClass") String animalClass) {
		String resultMessage = Tc.EMPTY_ARRAY_STRING;
		String publicDboUserName = null;
		DbDataArray arrResult = null;
		SvSecurity svs = null;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svs = new SvSecurity();
			((SvCore) svs).switchUser(svCONST.serviceUser);
			svr = new SvReader(svs);
			rdr = new Reader();
			publicDboUserName = SvConf.getParam(Tc.reporterUserParam);
			if (publicDboUserName != null) {
				svr.switchUser(rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, publicDboUserName, svr));
				DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass, true,
						svr);
				if (dboAnimal != null) {
					DbDataObject dboHolding = svr.getObjectById(dboAnimal.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					dboAnimal.setVal(Tc.HOLDING_ID, dboHolding.getVal(Tc.PIC));
					arrResult = new DbDataArray();
					arrResult.addDataItem(dboAnimal);
					resultMessage = rdr.convertDbDataArrayToGridJson(arrResult, Tc.ANIMAL, true, svr);
				}
			}
		} catch (SvException e) {
			resultMessage = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null) {
				svs.release();
			}
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMessage).build();
	}

	@Path("/searchAnimalById/{animalId}/{localeId}")
	@GET
	@Produces("application/json")
	public Response searchAnimalById(@PathParam("animalId") String animalId, @PathParam("localeId") String localeId)
			throws ParseException {
		// String finalResult = Tc.EMPTY_ARRAY_STRING;
		JsonObject finalResult = new JsonObject();
		SvReader svr = null;
		Reader rdr = null;
		PublicServiceDataTranslator psdt = null;
		try {
			rdr = new Reader();
			psdt = new PublicServiceDataTranslator();
			setUpLocale(localeId);
			svr = setUpUser(rdr);
			DbDataArray dbArrAnimals = rdr.findAnimalByAnimalId(animalId, svr);
			if (dbArrAnimals != null && !dbArrAnimals.getItems().isEmpty()) {
				for (DbDataObject dboAnimal : dbArrAnimals.getItems()) {
					JsonObject tempAnimalData = new JsonObject();
					// getBasicAnimalData
					psdt.loadAnimalDataInJson(tempAnimalData, dboAnimal, localeId, rdr, svr);
					// readHoldingData
					psdt.loadHoldingDataForAnimalinJson(tempAnimalData, dboAnimal, localeId, rdr, svr);
					psdt.loadPostMortemDataForAnimalInJson(tempAnimalData, dboAnimal, localeId, rdr, svr);
					tempAnimalData.add(Tc.HOLDING_VISITS,
							psdt.loadHoldingHistoryForAnimal2(dboAnimal, localeId, rdr, svr));
					tempAnimalData.add(Tc.VACC_CAMPAIGNS,
							psdt.loadVaccineHistoryPerDiseaseForAnimal(dboAnimal, localeId, rdr, svr));
					finalResult.add(dboAnimal.getObject_id().toString(), tempAnimalData);

				}
			}
			log4j.trace(finalResult);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null)
				svr.release();
		}
		return Response.status(200).entity(finalResult.toString()).build();
	}

	@Path("/getPostMortemPdfForAnimal/{animalId}/{animalClass}")
	@GET
	@Produces("application/pdf")
	public StreamingOutput getPostMortemPdfForAnimal(@PathParam("animalId") String animalId,
			@PathParam("animalClass") String animalClass) {
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException {
				String publicUsername = null;
				SvSecurity svs = null;
				SvReader svr = null;
				SvWriter svw = null;
				Properties prop = null;
				Reader rdr = null;
				try {
					publicUsername = SvConf.getParam(Tc.reporterUserParam);
					svs = new SvSecurity();
					((SvCore) svs).switchUser(svCONST.serviceUser);
					svr = new SvReader(svs);
					svw = new SvWriter(svr);
					rdr = new Reader();
					prop = new Properties();
					if (publicUsername == null) {
						svr.switchUser(
								rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, Tc.reporterUserName, svr));
					} else {
						svr.switchUser(
								rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, publicUsername, svr));
					}
					DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass,
							true, svr);
					if (dboAnimal.getVal(Tc.STATUS) != null && !dboAnimal.getVal(Tc.STATUS).equals(Tc.POSTMORTEM)) {
						throw (new SvException("naits.error.animalHasNoPostmortemStatusToShowThePostmortemForm",
								svw.getInstanceUser()));
					}
					DateTime refDate = new DateTime();
					String printParam = SvConf.getParam(Tc.printJrxmlPath);
					prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
					prop.put(Tc.printJrxmlPath, printParam);
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					IOUtils.write(data, output);
					GeneratePdf.executeReport(prop, "slaugh_6in1_final_geo", Tc.PDF, refDate, output, svr.dbGetConn());

				} catch (Exception e) {
					log4j.error("Error printing PDF method: generatePdf", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svw != null)
						svw.release();
				}

			}
		};
	}

	/**
	 * Method that returns PDF file with animal details, without user authorization
	 *
	 * @param animalId
	 * @param animalClass
	 * @param reportName
	 * @return
	 */
	@Path("/getAnimalDetailsInPdfFormat/{animalId}/{animalClass}/{reportName}/{companyName}")
	@GET
	@Produces("application/pdf")
	public StreamingOutput getAnimalDetailsInPdfFormat(@PathParam("animalId") String animalId,
			@PathParam("animalClass") String animalClass, @PathParam("reportName") String reportName,
			@PathParam("companyName") String companyName) {
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException {
				String publicUsername = null;
				SvSecurity svs = null;
				SvReader svr = null;
				Properties prop = null;
				Reader rdr = null;
				try {
					publicUsername = SvConf.getParam(Tc.reporterUserParam);
					svs = new SvSecurity();
					((SvCore) svs).switchUser(svCONST.serviceUser);
					svr = new SvReader(svs);
					rdr = new Reader();
					prop = new Properties();
					if (publicUsername == null) {
						svr.switchUser(
								rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, Tc.reporterUserName, svr));
					} else {
						svr.switchUser(
								rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, publicUsername, svr));
					}
					DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass,
							true, svr);
					DateTime refDate = new DateTime();
					String path = SvConf.getParam(Tc.frontHostParam);
					if (path.equalsIgnoreCase(Tc.appProd1) || path.equalsIgnoreCase(Tc.appProd2)
							|| path.equalsIgnoreCase(Tc.appProd3)) {
						path = "https://naits.mepa.gov.ge/triglav_rest/";
					}
					String fullPath = path + Tc.classPublicServicesGeneratePdf + Tc.PATH_DELIMITER + animalId
							+ Tc.PATH_DELIMITER + animalClass + Tc.PATH_DELIMITER;
					String printParam = SvConf.getParam(Tc.printJrxmlPath);
					prop.put(Tc.printJrxmlPath, printParam);
					switch (reportName) {
					case "public_animal_details_main":
						prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
						prop.put("LINK_TO_POSTMORTEM_REPORT", fullPath + "slaugh_6in1_final_geo");
						break;
					case "slaugh_1in1_final_geo":
						prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
						prop.put("QR_CODE", fullPath + "public_animal_details_main/null");
						prop.put(Tc.COMPANY_NAME, companyName);
						break;
					case "master_1in1_final_geo":
						prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
						prop.put("QR_CODE", fullPath + "public_animal_details_main/null");
						break;
					case "slaugh_6in1_final_geo":
						prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
						prop.put(Tc.COMPANY_NAME, companyName);
						break;
					case "slaugh_8in1_final_geo":
						prop.put(Tc.OBJ_ID, dboAnimal.getObject_id());
						break;
					}
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					IOUtils.write(data, output);
					GeneratePdf.executeReport(prop, reportName, Tc.PDF, refDate, output, svr.dbGetConn());
				} catch (Exception e) {
					log4j.error("Error printing PDF method: generatePdf", e);
				} finally {
					if (svr != null)
						svr.release();
					if (svs != null)
						svs.release();
				}
			}
		};
	}

	// HOLDING

	/**
	 * Method that returns basic holding details by Holding ID/PIC in appropriate
	 * Json format
	 * 
	 * @param sessionId
	 * @param holdingId
	 * @return
	 */
	@Path("/searchHoldingById/{sessionId}/{holdingId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchHoldingById(@PathParam("sessionId") String sessionId,
			@PathParam("holdingId") String holdingId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrHoldings = rdr.findDataPerSingleFilter(Tc.PIC, holdingId, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			if (arrHoldings != null && !arrHoldings.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrHoldings, Tc.HOLDING, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns basic holding details by Holding Name in appropriate Json
	 * format
	 * 
	 * @param sessionId
	 * @param holdingId
	 * @return
	 */
	@Path("/searchHoldingByName/{sessionId}/{holdingName}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchHoldingByName(@PathParam("sessionId") String sessionId,
			@PathParam("holdingName") String holdingName) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrHoldings = rdr.findDataPerSingleFilter(Tc.NAME, holdingName, DbCompareOperand.ILIKE,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			if (arrHoldings != null && !arrHoldings.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrHoldings, Tc.HOLDING, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns holding details as summary info
	 * 
	 * @param sessionId
	 * @param holdingId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Path("/getHoldingDetails/{sessionId}/{holdingId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getHoldingDetails(@PathParam("sessionId") String sessionId,
			@PathParam("holdingId") String holdingId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrHoldings = rdr.findDataPerSingleFilter(Tc.PIC, holdingId, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			if (arrHoldings != null && !arrHoldings.getItems().isEmpty()) {
				JSONObject jObj = new JSONObject();
				DbDataObject dboHolding = arrHoldings.get(0);
				LinkedHashMap<String, String> jsonOrderedMap = rdr.generateHoldingSummaryInformation(dboHolding, svr);
				jObj.put("holdingDetails", jsonOrderedMap);
				result = jObj.toJSONString();
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that edit holding data
	 * 
	 * @param sessionId
	 * @param pic
	 * @param name
	 * @param type
	 * @param physicalAddress
	 * @param villageCode
	 * @return
	 */
	@Path("/editHoldingData/{sessionId}/{pic}/{name}/{type}/{physicalAddress}/{villageCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response editHoldingData(@PathParam("sessionId") String sessionId, @PathParam("pic") String pic,
			@PathParam("name") String name, @PathParam("type") String type,
			@PathParam("physicalAddress") String physicalAddress, @PathParam("villageCode") String villageCode) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		Writer wr = null;
		DbDataObject dboHolding = null;
		DbDataArray dbArray = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			wr = new Writer();
			dboHolding = new DbDataObject();
			dbArray = new DbDataArray();
			DbDataArray arrHoldings = rdr.findDataPerSingleFilter(Tc.PIC, pic, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			if (arrHoldings != null && !arrHoldings.getItems().isEmpty()) {
				dboHolding = arrHoldings.get(0);
				wr.createOrUpdateHoldingData(dboHolding, name, type, physicalAddress, villageCode, svr);
				dbArray.addDataItem(dboHolding);
				result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.HOLDING, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that add holding data
	 * 
	 * @param sessionId
	 * @param name
	 * @param type
	 * @param physicalAddress
	 * @param villageCode
	 * @return
	 */
	@Path("/addHoldingData/{sessionId}/{name}/{type}/{physicalAddress}/{villageCode}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response addHoldingData(@PathParam("sessionId") String sessionId, @PathParam("name") String name,
			@PathParam("type") String type, @PathParam("physicalAddress") String physicalAddress,
			@PathParam("villageCode") String villageCode) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Writer wr = null;
		Reader rdr = null;
		DbDataObject dboHolding = null;
		DbDataArray dbArray = null;
		try {
			svr = new SvReader(sessionId);
			wr = new Writer();
			rdr = new Reader();
			dboHolding = new DbDataObject();
			dbArray = new DbDataArray();
			dboHolding = wr.createOrUpdateHoldingData(null, name, type, physicalAddress, villageCode, svr);
			dbArray.addDataItem(dboHolding);
			result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.HOLDING, true, svr);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns list of animals in Json format by holding ID
	 * 
	 * @param sessionId
	 * @param holdingId
	 * @return
	 */
	@Path("/getAnimalListByHoldingId/{sessionId}/{holdingId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAnimalListByHoldingId(@PathParam("sessionId") String sessionId,
			@PathParam("holdingId") String holdingId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrHoldings = rdr.findDataPerSingleFilter(Tc.PIC, holdingId, DbCompareOperand.EQUAL,
					SvReader.getTypeIdByName(Tc.HOLDING), svr);
			if (arrHoldings != null && !arrHoldings.getItems().isEmpty()) {
				DbDataObject dboHolding = arrHoldings.get(0);
				DbDataArray arrAnimals = svr.getObjectsByParentId(dboHolding.getObject_id(),
						SvReader.getTypeIdByName(Tc.ANIMAL), null, 0, 0);
				if (!arrAnimals.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(arrAnimals, Tc.ANIMAL, true, svr);
				}
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method used for searching Holdings by keeper ID/National number and returns
	 * appropriate response in Json format
	 * 
	 * @param sessionId
	 * @param personNatRegNumber
	 * @return
	 */
	@Path("/searchHoldingByPersonId/{sessionId}/{personNatRegNumber}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchHoldingByPersonId(@PathParam("sessionId") String sessionId,
			@PathParam("personNatRegNumber") String personNatRegNumber) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrHoldingsByKeeper = rdr
					.getLinkedHoldingsByKeeperNationalRegistrationNumber(personNatRegNumber, svr);
			if (arrHoldingsByKeeper != null && !arrHoldingsByKeeper.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrHoldingsByKeeper, Tc.HOLDING, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	// ANIMAL/FLOCK MOVEMENTS AND MOVEMENT DOCUMENTS

	/**
	 * Method used for searching movement documents by movement document ID and
	 * returns appropriate response in Json format
	 * 
	 * @param sessionId
	 * @param movementDocumentId
	 * @return
	 */
	@Path("/searchMovementDocumentById/{sessionId}/{movementDocumentId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchMovementDocumentById(@PathParam("sessionId") String sessionId,
			@PathParam("movementDocumentId") String movementDocumentId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrMovementDocuments = rdr.findDataPerSingleFilter(Tc.MOVEMENT_DOC_ID, movementDocumentId,
					DbCompareOperand.EQUAL, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC), svr);
			if (arrMovementDocuments != null && !arrMovementDocuments.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrMovementDocuments, Tc.MOVEMENT_DOC, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns list of movement documents in appropriate Json format by
	 * transporter license
	 * 
	 * @param sessionId
	 * @param transporterLicense
	 * @return
	 */
	@Path("/searchMovementDocumentByTransporterLicense/{sessionId}/{transporterLicense}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchMovementDocumentByTransporterLicense(@PathParam("sessionId") String sessionId,
			@PathParam("transporterLicense") String transporterLicense) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrMovementDocuments = rdr.getMovementDocumentByTransporterLicense(transporterLicense, svr);
			if (arrMovementDocuments != null && !arrMovementDocuments.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrMovementDocuments, Tc.MOVEMENT_DOC, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	// VACCINATION EVENTS

	/**
	 * Method that returns list of active Vaccination events/Campaigns
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getListOfActiveCampaignEvents/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getListOfActiveCampaignEvents(@PathParam("sessionId") String sessionId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataArray arrActiveCampaigns = rdr.getValidVaccEvents(svr, null);
			if (arrActiveCampaigns != null && !arrActiveCampaigns.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(arrActiveCampaigns, Tc.VACCINATION_EVENT, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns animal post-mortem form by animal id and animal class,
	 * with animal details, name of the slaughterhouse where it was slaughtered,
	 * post-mortem form details and link to slaughter 6in1 report
	 * 
	 * @param sessionId
	 * @param animalId
	 * @param animalClass
	 * @return
	 * @throws ParseException
	 */
	@Path("/getPostMortemFormByAnimalEarTag/{sessionId}/{animalId}/{animalClass}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPostMortemFormByAnimalEarTag(@PathParam("sessionId") String sessionId,
			@PathParam("animalId") String animalId, @PathParam("animalClass") String animalClass) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArray = new DbDataArray();
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass, true,
					svr);
			if (dboAnimal != null) {
				String path = SvConf.getParam(Tc.frontHostParam);
				Long objectId = dboAnimal.getObject_id();
				String fullPath = path + Tc.classReportMethodGeneratePdf + sessionId + Tc.PATH_DELIMITER + objectId
						+ Tc.PATH_DELIMITER + Tc.slaugh_6in1_final_geo;
				DbDataArray arrPostMortemForm = svr.getObjectsByParentId(dboAnimal.getObject_id(),
						SvReader.getTypeIdByName(Tc.POST_SLAUGHT_FORM), null, 0, 0);
				if (arrPostMortemForm != null && !arrPostMortemForm.getItems().isEmpty()) {
					DbDataObject dboPostMortemForm = arrPostMortemForm.get(0);
					dboPostMortemForm.setVal(Tc.LINK_TO_POSTMORTEM_REPORT, fullPath);
					DbDataObject dboSlaughterHouse = svr.getObjectById(dboAnimal.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
					dboAnimal.setVal(Tc.NAME, dboSlaughterHouse.getVal(Tc.NAME));
					dbArray.addDataItem(dboAnimal);
					dbArray.addDataItem(dboPostMortemForm);
					result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.POST_SLAUGHT_FORM, true, svr);
				}
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns animal details in JSON format, without authorization
	 * 
	 * @param animalId
	 * @param animalClass
	 * @return
	 */
	@Path("/getAnimalDetails/{animalId}/{animalClass}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAnimalDetails(@PathParam("animalId") String animalId,
			@PathParam("animalClass") String animalClass) {
		String result = Tc.EMPTY_ARRAY_STRING;
		String publicDboUserName = null;
		DbDataArray dbArray = null;
		SvSecurity svs = null;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svs = new SvSecurity();
			((SvCore) svs).switchUser(svCONST.serviceUser);
			svr = new SvReader(svs);
			rdr = new Reader();
			publicDboUserName = SvConf.getParam(Tc.reporterUserParam);
			if (publicDboUserName != null) {
				svr.switchUser(rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, publicDboUserName, svr));
				DbDataObject dboAnimal = rdr.findAppropriateAnimalByAnimalIdAndAnimalClass(animalId, animalClass, true,
						svr);
				dbArray = new DbDataArray();
				dbArray.addDataItem(dboAnimal);
				result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.ANIMAL, true, svr);
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svs != null) {
				svs.release();
			}
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns LAB SAMPLE details per animal_id & animal_class in JSON
	 * format, with authorization
	 * 
	 * @param sessionId
	 * @param animalId
	 * @param animalClass
	 * @return
	 */
	@Path("/getLabSampleDetails/{sessionId}/{animalId}/{animalClass}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLabSampleDetails(@PathParam("sessionId") String sessionId,
			@PathParam("animalId") String animalId, @PathParam("animalClass") String animalClass) {
		String result = Tc.EMPTY_ARRAY_STRING;
		DbDataArray dbArray = null;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			dbArray = rdr.findLabSamplesPerAnimalIdAndAnimalClass(animalId, animalClass, true, svr);
			result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.LAB_SAMPLE, true, svr);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that returns LAB SAMPLE_RESULT details per animal_id & animal_class in
	 * JSON format, with authorization
	 * 
	 * @param sessionId
	 * @param animalId
	 * @param animalClass
	 * @return
	 */
	@Path("/getLabSampleResults/{sessionId}/{sampleId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLabSampleResults(@PathParam("sessionId") String sessionId,
			@PathParam("sampleId") String sampleId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		DbDataArray dbArray = null;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			dbArray = rdr.findLabSampleResultsPerSampleId(sampleId, svr);
			result = rdr.convertDbDataArrayToGridJson(dbArray, Tc.LAB_TEST_RESULT, true, svr);
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that creates LAB SAMPLE details per animal, with authorization
	 * 
	 * @param sessionId
	 * @param animalId
	 * @param animalClass
	 * @return
	 */
	@Path("/createLabSample/{sessionId}/{diseaseTest}/{dateOfCollection}/{animalEarTag}/{animalType}/{collectionerName}/{hodlingPic}/{holdingResponsible}/{labName}/{sampleType}/{sampleTestType}/{sampleOrigin}/{testResultStatus}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response createLabSample(@PathParam("sessionId") String sessionId,
			@PathParam("diseaseTest") String diseaseTest, @PathParam("dateOfCollection") String dateOfCollection,
			@PathParam("animalEarTag") String animalEarTag, @PathParam("animalType") String animalType,
			@PathParam("collectionerName") String collectionerName, @PathParam("hodlingPic") String hodlingPic,
			@PathParam("holdingResponsible") String holdingResponsible, @PathParam("labName") String labName,
			@PathParam("sampleType") String sampleType, @PathParam("sampleTestType") String sampleTestType,
			@PathParam("sampleOrigin") String sampleOrigin, @PathParam("testResultStatus") String testResultStatus) {
		String result = "naits.success.labSampleCreated";
		SvReader svr = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			wr = new Writer();
			DbDataObject dboLabSample = wr.createLabSample(diseaseTest, dateOfCollection, animalEarTag, animalType,
					collectionerName, hodlingPic, holdingResponsible, labName, sampleType, sampleTestType, sampleOrigin,
					testResultStatus, svr);
			if (dboLabSample == null || dboLabSample.getObject_id().equals(0L)) {
				result = "naits.error.labSampleNotCreated";
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that creates LAB SAMPLE_RESULT details per LAB_SAMPLE, with
	 * authorization
	 * 
	 * @param sessionId
	 * @param sampleId
	 * @param testType
	 * @param testName
	 * @param testDate
	 * @param testResult
	 * @param sampleDisease
	 * @return
	 */
	@Path("/createLabSampleResult/{sessionId}/{sampleId}/{testType}/{testName}/{testDate}/{testResult}/{sampleDisease}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response createLabSampleResult(@PathParam("sessionId") String sessionId,
			@PathParam("sampleId") String sampleId, @PathParam("testType") String testType,
			@PathParam("testName") String testName, @PathParam("testDate") String testDate,
			@PathParam("testResult") String testResult, @PathParam("sampleDisease") String sampleDisease) {
		String result = "naits.success.labSampleResultCreated";
		SvReader svr = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			wr = new Writer();
			DbDataObject dboLabSampleResult = wr.createLabSampleResult(sampleId, testType, testName, testDate,
					testResult, sampleDisease, svr);
			if (dboLabSampleResult == null || dboLabSampleResult.getObject_id().equals(0L)) {
				result = "naits.error.labSampleResultNotCreated";
			}
		} catch (SvException e) {
			result = e.getLabelCode();
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}
}
