package naits_triglav_plugin;

import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

@Path("/naits_triglav_plugin/HerdServices")
public class HerdServices {

	static final Logger log4j = LogManager.getLogger(HerdServices.class.getName());
	
	static final boolean isInitialized = initPlugin();
	
	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HERD));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HERD_MOVEMENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HERD_HEALTH_BOOK));
		return true;
	}
	
	@Path("/addAnimalToHerd/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response addAnimalToHerd(@PathParam("sessionId") String sessionId, MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.massHerdAction";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		HerdActions ha = new HerdActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = ha.herdMassHandler(jsonData, sessionId);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("Error in processing addAnimalToHerd:" + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing addAnimalToHerd:", e);
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}
	
	@Path("/getAllAvailableAnimalsPerSelectedType/{sessionId}/{herdObjId}/{animalType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAllAvailableAnimalsPerSelectedType(@PathParam("sessionId") String sessionId,
			@PathParam("herdObjId") Long herdObjId, @PathParam("animalType") String animalType) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			if (herdObjId != null && animalType != null && !animalType.isEmpty()
					&& (animalType.equals("1") || animalType.equals("9") || animalType.equals("10"))) {
				DbDataObject dboHerd = svr.getObjectById(herdObjId, SvReader.getTypeIdByName(Tc.HERD), null);
				DbDataArray dbArrRes = rdr.getAllAvailableAnimalsPerSelectedType(dboHerd, animalType, svr);
				if (dbArrRes != null && !dbArrRes.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(dbArrRes, Tc.ANIMAL, false, Tc.PKID, Tc.DESC, svr);
				}
			}
		} catch (Exception e) {
			log4j.error("General error in processing getAllAvailableAnimalsPerSelectedType:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}
	
	@Path("/addHerdToHolding/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response addHerdToHolding(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToAddHerdToHolding";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		HerdActions ha = new HerdActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				result = ha.addHerdToHolding(jsonData, sessionId);
			} else {
				result = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			result = ((SvException) e).getLabelCode();
			log4j.error("Error in processing addHerdToHolding:" + ((SvException) e).getFormattedMessage(), e);
		}
		return Response.status(200).entity(result).build();
	}
	
	@Path("/removeAnimalFromHerd/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response removeAnimalFromHerd(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToRemoveAnimalFromHerd";
		JsonObject jsonData = null;
		Gson gson = new Gson();
		HerdActions ha = new HerdActions();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				result = ha.herdMassHandler(jsonData, sessionId);
			} else {
				result = Tc.error_admConsoleBadJson;
			}
		} catch (SvException e) {
			result = ((SvException) e).getLabelCode();
			log4j.error("Error in processing removeAnimalFromHerd:" + ((SvException) e).getFormattedMessage(), e);
		}
		return Response.status(200).entity(result).build();
	}
	
	@Path("/herdMassActions/{sessionId}/{herdObjId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response herdMassActions(@PathParam("sessionId") String sessionId, @PathParam("herdObjId") Long herdObjId,
			MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.massHerdAction";
		JsonObject jsonData = null;
		Gson gson = null;
		MassActions ma = null;
		HerdActions ha = null;
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		JsonArray jArrOfAnimals = null;
		Writer wr = null;

		String action = null;
		String subAction = null;
		String sub_action = null;
		String dateOfAction = null;
		String movementReason = null;
		String estmDepartureDate = null;
		String estmArrivalDate = null;
		String dateOfMovement = null;
		String movementType = null;
		String transporterLicense = null;
		String transporterId = null;
		String transporterName = null;
		String desinfectionDate = null;
		Long additionalParam = null;
		String vaccActivityType = null;
		DbDataObject movementDocument = null;
		String objectTypeLabel = Tc.herd;
		try {
			jsonData = new JsonObject();
			gson = new Gson();
			ma = new MassActions();
			ha = new HerdActions();
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			wr = new Writer();
			jArrOfAnimals = ha.prepareJsonObjectForHerdMassActions(herdObjId, svr);
			if (jArrOfAnimals.size() > 0) {
				for (Entry<String, List<String>> entry : formVals.entrySet()) {
					if (entry.getKey() != null && !entry.getKey().isEmpty()) {
						String key = entry.getKey();
						jsonData = gson.fromJson(key, JsonObject.class);
					}
				}
				action = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ACTION).toUpperCase();
				subAction = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_SUBACTION).toUpperCase();
				sub_action = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_SUB_ACTION);
				dateOfAction = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ACTION_DATE);
				if (!ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ADDITIONAL_PARAM).isEmpty()) {
					additionalParam = Long.valueOf(ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ADDITIONAL_PARAM));
				}
				Long headCount = ha.getSizeOfListAnimalsInHerd(herdObjId, svr);
				movementReason = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ANIMAL_MVM_REASON);
				estmDepartureDate = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ESTM_DATE_OF_DEPARTURE);
				estmArrivalDate = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_ESTM_DATE_OF_ARRIVAL);
				dateOfMovement = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_DATE_OF_MOVEMENT);
				movementType = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_MVM_TRANSPORT_TYPE);
				transporterLicense = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_TRANSPORTER_LICENCE);
				transporterId = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_TRANSPORTER_PERSON_ID);
				transporterName = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_TRANSPORTER_NAME);
				desinfectionDate = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_DISINFECTION_DATE);
				vaccActivityType = ha.getValueFromJson(jsonData, Tc.MASS_PARAM_VACC_ACTIVITY_TYPE);
				jsonData.add(Tc.OBJ_ARRAY, (JsonElement) jArrOfAnimals);
				if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
					resultMsgLbl = ma.animalFlockMassHandler(jsonData, sessionId);
					if (resultMsgLbl.equals(Tc.successMassAnimalsAction)) {
						switch (action) {
						case Tc.RETIRE:
							DbDataObject dboHerd = svr.getObjectById(herdObjId, SvReader.getTypeIdByName(Tc.HERD),
									null);
							DbDataObject dboHerdMovement = null;
							if (subAction.equals(Tc.SOLD) || subAction.equals(Tc.LOST)) {
								dboHerdMovement = ha.createHerdMovementWithoutDestination(dboHerd, subAction, null, svr,
										svw, sww);
								svw.saveObject(dboHerdMovement);
							} else {
								sww.moveObject(dboHerd, subAction);
							}
							break;
						case Tc.ACTIVITY:
							DateTime dtAction = new DateTime(dateOfAction);
							if (dtAction.isAfter(new DateTime())) {
								throw (new SvException("naits.error.actionDateCannotBeInTheFuture",
										svr.getInstanceUser()));
							}
							switch (subAction) {
							case Tc.PHYSICAL_CHECK:
								DbDataObject dboHerdHealthBook = ha.createHerdHealthBook(herdObjId, dateOfAction,
										vaccActivityType, Tc.STATE, "Physical check", headCount, svr);
								svw.saveObject(dboHerdHealthBook);
								break;
							default:
								if (additionalParam != null) {
									DbDataObject dboCampaign = svr.getObjectById(additionalParam,
											SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);
									if (dboCampaign != null && dboCampaign.getVal(Tc.ACTIVITY_TYPE) != null) {
										DbDataObject dboHerdHealthBookVaccine = ha.createHerdHealthBook(herdObjId,
												dateOfAction, dboCampaign.getVal(Tc.ACTIVITY_TYPE).toString(), Tc.STATE,
												"Activity", headCount, svr);
										svw.saveObject(dboHerdHealthBookVaccine);

										DbDataObject dbLinkCampaignHerdHealthBook = SvReader.getLinkType(
												Tc.VACC_BOOK_HERD_HEALTH_BOOK,
												SvReader.getTypeIdByName(Tc.VACCINATION_BOOK),
												SvReader.getTypeIdByName(Tc.HERD_HEALTH_BOOK));
										DbDataObject dboLink = wr.createSvarogLink(
												dbLinkCampaignHerdHealthBook.getObject_id(), dboCampaign,
												dboHerdHealthBookVaccine);
										svw.saveObject(dboLink);
									}
								}
								break;
							}
						default:
							break;
						}
					}
					if (resultMsgLbl.startsWith("naits.success.")) {
						switch (action) {
						case Tc.MOVE:
							switch (subAction) {
							case Tc.START_MOVEMENT:
								if (additionalParam != null) {
									DbDataObject dboDestinationHolding = svr.getObjectById(additionalParam,
											SvReader.getTypeIdByName(Tc.HOLDING), null);
									DbDataObject dboHerdObject = svr.getObjectById(herdObjId,
											SvReader.getTypeIdByName(Tc.HERD), null);
									DbDataObject dboHerdParent = svr.getObjectById(dboHerdObject.getParent_id(),
											SvReader.getTypeIdByName(Tc.HOLDING), null);
									if (Tc.MOVE.equalsIgnoreCase(action)
											&& Tc.START_MOVEMENT.equalsIgnoreCase(subAction)
											&& !dboHerdParent.getObject_id().equals(0L)
											&& dboHerdParent.getObject_id() != null) {
										movementDocument = wr.createMovementDocument(dboHerdParent.getObject_id(), svr);
										movementDocument.setVal(Tc.MOVEMENT_TYPE, objectTypeLabel.toUpperCase());
										movementDocument.setVal(Tc.DESTINATION_HOLDING_PIC,
												dboDestinationHolding.getVal(Tc.PIC) != null
														? dboDestinationHolding.getVal(Tc.PIC).toString() : null);
										if (movementDocument != null) {
											svw.saveObject(movementDocument);
										}
									}
									ha.startHerdMovement(dboHerdObject,
											movementDocument.getVal(Tc.MOVEMENT_DOC_ID).toString(), movementReason, "",
											dboDestinationHolding, estmDepartureDate, estmArrivalDate, dateOfMovement,
											movementType, transporterId, transporterName, transporterLicense,
											desinfectionDate, sww, svw, svr);
									resultMsgLbl = "naits.success.massAnimalsAction";
								}
								break;
							case Tc.CANCEL_MOVEMENT:
								DbDataObject dboHerdObject = svr.getObjectById(herdObjId,
										SvReader.getTypeIdByName(Tc.HERD), null);
								ha.cancelMovement(dboHerdObject, sww, svr);
								resultMsgLbl = "naits.success.massAnimalsAction";
								break;
							default:
								break;
							}
						default:
							break;
						}
						if (subAction.equals(Tc.FINISH_MOVEMENT) && (sub_action.equals(Tc.ACCEPT_FULL_HERD)
								|| sub_action.equals(Tc.ACCEPT_FULL_HERD_INDIVIDUAL))) {
							switch (sub_action) {
							case Tc.ACCEPT_FULL_HERD:
								if (additionalParam != null) {
									DbDataObject dboDestinationHolding = svr.getObjectById(additionalParam,
											SvReader.getTypeIdByName(Tc.HOLDING), null);
									DbDataObject dboHerdObject = svr.getObjectById(herdObjId,
											SvReader.getTypeIdByName(Tc.HERD), new DateTime());
									ha.finishHerdMovement(dboHerdObject, dboDestinationHolding, dateOfMovement, null,
											null, sww, svr);
									dboHerdObject.setParent_id(dboDestinationHolding.getObject_id());
									dboHerdObject.setStatus(Tc.VALID);
									dboHerdObject.setVal(Tc.HERD_ID, Tc.EMPTY_STRING);
									svw.saveObject(dboHerdObject, false);
									DbDataArray animalsInHerd = ha.getAnimalsInHerd(dboHerdObject.getObject_id(), svr);
									if (animalsInHerd != null && !animalsInHerd.getItems().isEmpty()) {
										for (DbDataObject tempAnimal : animalsInHerd.getItems()) {
											sww.moveObject(tempAnimal, Tc.VALID);
										}
									}
									resultMsgLbl = "naits.success.massAnimalsAction";
								}
								break;
							case Tc.ACCEPT_FULL_HERD_INDIVIDUAL:
								if (additionalParam != null) {
									DbDataObject dboDestinationHolding = svr.getObjectById(additionalParam,
											SvReader.getTypeIdByName(Tc.HOLDING), null);
									DbDataObject dboHerdObject = svr.getObjectById(herdObjId,
											SvReader.getTypeIdByName(Tc.HERD), new DateTime());
									ha.finishHerdMovement(dboHerdObject, dboDestinationHolding, dateOfMovement, null,
											null, sww, svr);
									sww.moveObject(dboHerdObject, Tc.INVALID, false);
									DbDataArray animalsInHerd = ha.getAnimalsInHerd(dboHerdObject.getObject_id(), svr);
									if (animalsInHerd != null && !animalsInHerd.getItems().isEmpty()) {
										for (DbDataObject tempAnimal : animalsInHerd.getItems()) {
											if (ha.invalidateLinkBetweenAnimalAndHerd(tempAnimal,
													dboHerdObject.getObject_id(), svr)) {
												tempAnimal.setVal(Tc.IS_IN_HERD, null);
												tempAnimal.setStatus(Tc.VALID);
												svw.saveObject(tempAnimal);
											}
										}
									}
									resultMsgLbl = "naits.success.massAnimalsAction";
								}
								break;
							default:
								break;
							}
						}
					}
				}
			} else {
				resultMsgLbl = "naits.error.herdMustHaveAnimalToExecuteMassActions";
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			resultMsgLbl = ((SvException) e).getLabelCode();
			log4j.error("Error in processing herdMassActions:" + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing herdMassActions:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/getAllHoldingResponsiblesPerHerd/{sessionId}/{herdObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getAllHoldingResponsiblesPerHerd(@PathParam("sessionId") String sessionId,
			@PathParam("herdObjId") Long herdObjId) {
		String result = Tc.EMPTY_ARRAY_STRING;
		SvReader svr = null;
		HerdActions ha = null;
		Reader rdr = null;
		DbDataArray res = null;
		try {
			svr = new SvReader(sessionId);
			ha = new HerdActions();
			rdr = new Reader();
			res = new DbDataArray();
			if (herdObjId != null) {
				res = ha.getAllHoldingResponsiblesPerHerd(herdObjId, svr);
				if (res != null && !res.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(res, Tc.HOLDING_RESPONSIBLE);
				}
			}
		} catch (Exception e) {
			log4j.error("General error in processing getAllHoldingResponsiblesPerHerd:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}
	
	@Path("/finishIndividualMovement/{sessionId}/{herdObjId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response finishIndividualMovement(@PathParam("sessionId") String sessionId,
			@PathParam("herdObjId") Long herdObjId, MultivaluedMap<String, String> formVals) throws SvException {
		String result = Tc.error_admConsoleBadJson;
		SvReader svr = new SvReader(sessionId);
		SvWriter svw = new SvWriter(svr);
		SvWorkflow sww = new SvWorkflow(svw);
		Gson gson = new Gson();
		Writer wr = new Writer();
		JsonObject jsonData = null;
		MassActions ma = new MassActions();
		HerdActions ha = new HerdActions();
		String additionalSubActionParam = null;
		Long additionalParam = null;
		DbDataArray linksToBeSaved = new DbDataArray();
		try {
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY) && herdObjId != null) {
				DbDataObject dboHerd = svr.getObjectById(herdObjId, SvReader.getTypeIdByName(Tc.HERD), new DateTime());
				JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();
				for (JsonElement jsonElement : jsonParams) {
					JsonObject obj = jsonElement.getAsJsonObject();
					if (obj.has(Tc.MASS_PARAM_ADDITIONAL_PARAM)) {
						additionalParam = obj.get(Tc.MASS_PARAM_ADDITIONAL_PARAM).getAsLong();
					}
					if (obj.has(Tc.MASS_PARAM_SUB_ACTION)) {
						additionalSubActionParam = obj.get(Tc.MASS_PARAM_SUB_ACTION).getAsString().toUpperCase();
					}
				}
				DbDataObject dboDestinationHolding = svr.getObjectById(additionalParam,
						SvReader.getTypeIdByName(Tc.HOLDING), null);
				String resultAction = ma.animalFlockMassHandler(jsonData, sessionId);
				if (resultAction.startsWith("naits.success")) {
					switch (additionalSubActionParam) {
					case Tc.ACCEPT_INDIVIDUAL_ANIMAL:
						JsonArray jsonArrayData = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
						DbDataObject animalTypeDesc = SvReader.getDbtByName(Tc.ANIMAL);
						for (JsonElement jsonElement : jsonArrayData) {
							DbDataObject dboAnimal = null;
							JsonObject obj = jsonElement.getAsJsonObject();
							if (obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID) != null) {
								dboAnimal = svr.getObjectById(obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID).getAsLong(),
										animalTypeDesc, null);
								if (dboAnimal != null
										&& ha.invalidateLinkBetweenAnimalAndHerd(dboAnimal, herdObjId, svr)) {
									dboAnimal.setVal(Tc.IS_IN_HERD, null);
									dboAnimal.setStatus(Tc.VALID);
									svw.saveObject(dboAnimal);
								}
							}
						}
						DbDataArray animalsInHerd = ha.getAnimalsInHerd(herdObjId, svr);
						if (animalsInHerd == null || animalsInHerd.getItems().isEmpty()) {
							ha.finishHerdMovement(dboHerd, dboDestinationHolding, null, null, null, sww, svr);
							sww.moveObject(dboHerd, Tc.INVALID, false);
						}
						result = "naits.success.successfullyFinishedAction";
						break;
					case Tc.ACCEPT_INDIVIDUAL_ANIMAL_HERD:
						DbDataObject dboNewHerd = ha.createHerdWithoutHerdId(additionalParam,
								dboHerd.getVal(Tc.ANIMAL_TYPE).toString(), dboHerd.getVal(Tc.NAME).toString(),
								Long.valueOf(dboHerd.getVal(Tc.CONTACT_PERSON_ID).toString()), svr);
						svw.saveObject(dboNewHerd);
						JsonArray jsonArrayData1 = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
						DbDataObject animalTypeDesc1 = SvReader.getDbtByName(Tc.ANIMAL);
						for (JsonElement jsonElement : jsonArrayData1) {
							DbDataObject dboAnimal = null;
							JsonObject obj = jsonElement.getAsJsonObject();
							if (obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID) != null) {
								dboAnimal = svr.getObjectById(obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID).getAsLong(),
										animalTypeDesc1, null);
								if (dboAnimal != null
										&& ha.invalidateLinkBetweenAnimalAndHerd(dboAnimal, herdObjId, svr)) {
									DbDataObject dboLinkBetweenAnimalAndHerd = ha
											.createLinkBetweenAnimalAndHerd(dboAnimal, dboNewHerd, wr, svr);
									linksToBeSaved.addDataItem(dboLinkBetweenAnimalAndHerd);
								}
							}
						}
						if (!linksToBeSaved.getItems().isEmpty()) {
							svw.saveObject(linksToBeSaved, true, true);
							result = "naits.success.successfullyFinishedAction";
						}
						DbDataArray animalsInHerd1 = ha.getAnimalsInHerd(herdObjId, svr);
						if (animalsInHerd1 == null || animalsInHerd1.getItems().isEmpty()) {
							ha.finishHerdMovement(dboHerd, dboDestinationHolding, null, null, null, sww, svr);
							sww.moveObject(dboHerd, Tc.INVALID, false);
						}
						result = "naits.success.successfullyFinishedAction";
						break;
					default:
						break;
					}
				}
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			result = ((SvException) e).getLabelCode();
			log4j.error("Error in processing finishIndividualMovement:" + ((SvException) e).getFormattedMessage(), e);
		} catch (Exception e) {
			log4j.error("General error in processing finishIndividualMovement:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/updateStatusOfMovementDocument/{sessionId}/{status}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response updateStatusOfMovementDocument(@PathParam("sessionId") String sessionId,
			@PathParam("status") String status, MultivaluedMap<String, String> formVals) {
		String resultMsgLbl = "naits.error.changeStatus";
		SvReader svr = null;
		HerdActions ha = new HerdActions();
		JsonObject jsonData = null;
		Gson gson = new Gson();
		try {
			svr = new SvReader(sessionId);
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					jsonData = gson.fromJson(key, JsonObject.class);
				}
			}
			if (jsonData != null && jsonData.has(Tc.OBJ_ARRAY)) {
				resultMsgLbl = ha.updateStatusOfMovementDoc(jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray(), status, svr);
			} else {
				resultMsgLbl = Tc.error_admConsoleBadJson;
			}
		} catch (Exception e) {
			resultMsgLbl = "naits.error.general";
			if (e instanceof SvException) {
				if (((SvException) e).getLabelCode().equals("naits.info.cantChangeStatus")) {
					resultMsgLbl = "naits.info.cantChangeStatus";
				}
				log4j.error("Error in processing mass action for: " + ((SvException) e).getFormattedMessage(), e);
			} else {
				log4j.error("General error in processing mass action", e);
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(resultMsgLbl).build();
	}

	@Path("/assignHerdLabSampleToLaboratory/{sessionId}/{labSampleObjId}/{laboratoryName}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response assignHerdLabSampleToLaboratory(@PathParam("sessionId") String sessionId,
			@PathParam("labSampleObjId") Long labSampleObjId, @PathParam("laboratoryName") String laboratoryName) {
		String result = "naits.error.failedToAssignHerdLabSampleToLaboratory";
		SvReader svr = null;
		SvWriter svw = null;
		HerdActions ha = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			ha = new HerdActions();
			rdr = new Reader();
			if (labSampleObjId != null && laboratoryName != null && !laboratoryName.equals(Tc.EMPTY_STRING)) {
				result = ha.createLinkBetweenLaboratoryAndHerdLabSampleByLabName(labSampleObjId, laboratoryName, rdr,
						svw, svr);
			}
		} catch (SvException e) {
			log4j.error("General error in processing assignHerdLabSampleToLaboratory:", e);
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
}
