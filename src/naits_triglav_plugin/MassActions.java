package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvGeometry;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvNote;
import com.prtech.svarog.SvNotification;
import com.prtech.svarog.SvParameter;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWorkflow;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

public class MassActions {

	static final int COMMIT_COUNT = 1000;
	static final Logger log4j = LogManager.getLogger(MassActions.class.getName());

	static boolean initPlugin() {
		OnSaveValidations call = new OnSaveValidations();
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.FLOCK));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.LAB_SAMPLE));
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.OTHER_ANIMALS));
		SvCore.registerOnSaveCallback(call, svCONST.OBJECT_TYPE_LINK);
		SvCore.registerOnSaveCallback(call, SvReader.getTypeIdByName(Tc.RANGE));
		return true;
	}

	public void userMassHandler(String actionName, String subActionName, String actionNote, JsonArray jsonData,
			String sessionId) throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		SvNote svn = null;
		Writer wr = null;
		try {
			String action = actionName.toUpperCase();

			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);
			svn = new SvNote(sww);
			wr = new Writer();

			svr.setAutoCommit(false);
			svw.setAutoCommit(false);
			sww.setAutoCommit(false);
			svn.setAutoCommit(false);

			DbDataObject userTypeDesc = SvReader.getDbt(svCONST.OBJECT_TYPE_USER);
			DbDataObject currentDboUser = SvReader.getUserBySession(sessionId);

			// check if parent exist
			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(), userTypeDesc,
							null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_user_found", svr.getInstanceUser()));
				}

				// for each user (except the current user) in the jsonData
				// array, do appropriate action
				// the current user which is logged in can not do any action
				// implicitly for himself
				if (!currentDboUser.getObject_id().equals(dboObjectToHandle.getObject_id())) {
					userMassAction(dboObjectToHandle, action, subActionName, actionNote, svw, sww, svn, wr);
				}
				svw.dbCommit();
				sww.dbCommit();
				svn.dbCommit();
				@SuppressWarnings(Tc.UNUSED)
				DbDataObject dboUser = svr.getObjectById(dboObjectToHandle.getObject_id(),
						dboObjectToHandle.getObject_type(), new DateTime());
			}

		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
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
			if (svn != null) {
				svn.release();
			}
		}
	}

	public void userMassAction(DbDataObject dboUser, String action, String subActionName, String actionNote,
			SvWriter svw, SvWorkflow sww, SvNote svn, Writer wr) throws SvException {
		ReentrantLock lock = null;
		try {
			lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);

			if (lock == null) {
				throw (new SvException(Tc.objectUsedByOtherSession, svw.getInstanceUser()));
			}
			switch (action.toUpperCase()) {
			case "RESET_PASS":
				// TODO:Put the default password and its hash into
				// svarog.properties file
				dboUser.setVal(Tc.PASS_HSH, "480FA232A3FDDFA31A5696DB829A90D7");
				svw.saveObject(dboUser, false);
				break;
			case "CHANGE_STATUS":
				switch (subActionName.toUpperCase()) {
				case Tc.SUSPENDED:
					if (!dboUser.getStatus().equals(Tc.SUSPENDED)) {
						wr.changeStatus(dboUser, Tc.SUSPENDED, sww);
						if (actionNote != null && actionNote.trim().length() > 0)
							generateSuspensionNotePerUser(dboUser, actionNote, svn);
					} else {
						throw (new SvException("naits.error.statusAlreadySuspend", svw.getInstanceUser()));
					}
					break;
				case Tc.INACTIVE:
					if (!dboUser.getStatus().equals(Tc.INACTIVE))
						wr.changeStatus(dboUser, Tc.INACTIVE, sww);
					else
						throw (new SvException("naits.error.statusAlreadyInactive", svw.getInstanceUser()));
					break;
				case Tc.VALID:
					if (!dboUser.getStatus().equals(Tc.VALID))
						wr.changeStatus(dboUser, Tc.VALID, sww);
					else
						throw (new SvException("naits.error.statusAlreadyValid", svw.getInstanceUser()));
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}
		} finally {
			if (lock != null) {
				SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
			}
		}
	}

	public void inventoryItemsMassStatusChange(String newStatus, JsonArray jsonData, String sessionId)
			throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svr.setAutoCommit(false);
			svw.setAutoCommit(false);
			int commitCount = 0;
			DbDataObject tempInvItem = null;
			DbDataArray invItemsToSave = new DbDataArray();
			DbDataObject invItemDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.INVENTORY_ITEM));
			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				tempInvItem = null;
				if (obj.get("INVENTORY_ITEM.OBJECT_ID") != null) {
					tempInvItem = svr.getObjectById(obj.get("INVENTORY_ITEM.OBJECT_ID").getAsLong(), invItemDesc, null);
				}
				if (tempInvItem == null) {
					throw (new SvException("naits.error.no_inv_item_found", svr.getInstanceUser()));
				}
				if (tempInvItem.getVal(Tc.TAG_STATUS) != null
						&& tempInvItem.getVal(Tc.TAG_STATUS).toString().equals(newStatus)) {
					continue;
				}
				tempInvItem.setVal(Tc.TAG_STATUS, newStatus);
				invItemsToSave.addDataItem(tempInvItem);
				commitCount++;
				if (commitCount == 100) {
					svw.saveObject(invItemsToSave);
					svw.dbCommit();
					commitCount = 0;
					invItemsToSave = new DbDataArray();
				}
			}
			svw.saveObject(invItemsToSave);
			svw.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
	}

	public void generateSuspensionNotePerUser(DbDataObject dboUser, String suspensionNote, SvNote svn)
			throws SvException {
		ReentrantLock lock = null;
		try {
			lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
			if (lock == null) {
				throw (new SvException(Tc.objectUsedByOtherSession, svn.getInstanceUser()));
			}
			svn.setNote(dboUser.getObject_id(), Tc.SUSPENSION_NOTE, suspensionNote);
		} finally {
			if (lock != null) {
				SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
			}
		}
	}

	public String moveAndLinkAnimals(String actionName, String actionParam, JsonArray jsonData, String sessionId)
			throws SvException {
		String resultMessage = Tc.successMassAnimalsAction;
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			String regex = Tc.NUMBER_REGEX;

			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);

			DbDataObject animalTypeDesc = SvReader.getDbtByName(Tc.ANIMAL);
			DbDataObject exportCertTypeDesc = SvReader.getDbtByName(Tc.EXPORT_CERT);

			DbDataObject dboExportCertificate = null;
			if (actionParam.matches(regex)) {
				dboExportCertificate = svr.getObjectById(Long.valueOf(actionParam), exportCertTypeDesc, null);
			}
			if (dboExportCertificate == null) {
				throw (new SvException("naits.error.no_export_cert_found", svr.getInstanceUser()));
			}

			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("ANIMAL.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("ANIMAL.OBJECT_ID").getAsLong(), animalTypeDesc,
							null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_animal_found", svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					switch (actionName.toUpperCase()) {
					case "PENDINGEXPORT":
						wr.movePotentionalAnimalsToExportCertificate(dboObjectToHandle, dboExportCertificate, rdr, svr);
						break;
					case "EXORT":
						sww.moveObject(dboObjectToHandle, Tc.EXPORTED, false);
						break;
					case "CLOSE":
						sww.moveObject(dboObjectToHandle, Tc.VALID, false);
						break;
					default:
						break;
					}
				} finally {
					SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
				}
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
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
		return resultMessage;
	}

	public String labSampleHandler(String actionName, String subActionName, String actionParam, JsonArray jsonData,
			String sessionId) throws SvException {
		String resultMessage = "naits.success.labSampleMassAction";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Writer wr = new Writer();
		Reader rdr = new Reader();
		try {
			svr = new SvReader(sessionId);
			svr.setAutoCommit(false);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);
			DbDataObject labSampleTypeDesc = SvReader.getDbtByName(Tc.LAB_SAMPLE);

			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("LAB_SAMPLE.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("LAB_SAMPLE.OBJECT_ID").getAsLong(),
							labSampleTypeDesc, new DateTime());
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_sample_found", svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					switch (actionName.toUpperCase()) {
					case Tc.CHANGE_THE_STATUS_OF_LAB_SAMPLE:
						switch (subActionName.toUpperCase()) {
						case Tc.RECEIVED:
							dboObjectToHandle.setStatus(Tc.RECEIVED);
							svw.saveObject(dboObjectToHandle, false);
							break;
						case Tc.REJECTED:
							wr.changeStatus(dboObjectToHandle, Tc.REJECTED, sww);
							break;
						default:
							break;
						}
						break;
					case Tc.SAMPLE_ACTION:
						if (Tc.ASSIGN_LAB.equals(subActionName)) {
							if (actionParam == null || actionParam.equals("")) {
								throw (new SvException("naits.error.cantAssignWithoutLaboratory",
										svr.getInstanceUser()));
							}
							resultMessage = wr.createLinkBetweenLaboratoryAndLabSampleByLabName(dboObjectToHandle,
									actionParam, rdr, svr, svw);
						}
						break;
					case Tc.SET_HEALTH_STATUS_TO_RESULTS:
						switch (subActionName.toUpperCase()) {
						case Tc.NEGATIVE:
							resultMessage = wr.setHealthStatusOfTestResults(dboObjectToHandle.getObject_id(), "1", svr,
									svw);
							break;
						case Tc.POSITIVE:
							resultMessage = wr.setHealthStatusOfTestResults(dboObjectToHandle.getObject_id(), "0", svr,
									svw);
							break;
						case Tc.INCONCLUSIVE:
							resultMessage = wr.setHealthStatusOfTestResults(dboObjectToHandle.getObject_id(), "2", svr,
									svw);
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
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
		return resultMessage;
	}

	public String exportCertMassHandler(JsonArray jsonData, String sessionId, Long exportCertObjectId)
			throws SvException {
		String resultMessage = "naits.success.exportAnimal";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			sww = new SvWorkflow(svw);

			vc = new ValidationChecks();

			DbDataObject dboAnimalDesc = SvReader.getDbtByName(Tc.ANIMAL);
			DbDataObject dboExportCertificate = svr.getObjectById(exportCertObjectId,
					SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
			DbDataObject dboQuarantine = svr.getObjectById(dboExportCertificate.getParent_id(),
					SvReader.getTypeIdByName(Tc.QUARANTINE), null);
			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				String dboToHandleStatus = Tc.EMPTY_STRING;
				DbDataObject dboObjectToHandle = null;
				if (obj.get("ANIMAL.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("ANIMAL.OBJECT_ID").getAsLong(), dboAnimalDesc,
							new DateTime());
				}
				if (dboExportCertificate == null) {
					throw (new SvException("naits.error.exportCertDoesntExist", svr.getInstanceUser()));
				}
				if (obj.get("ANIMAL.STATUS") != null) {
					dboToHandleStatus = obj.get("ANIMAL.STATUS").getAsString();
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_animal_found", svr.getInstanceUser()));
				}
				if (dboExportCertificate.getStatus().equals(Tc.PROCESSED)) {
					throw (new SvException("naits.info.exportCertAlreadyProcessed", svr.getInstanceUser()));
				}
				if (dboExportCertificate.getStatus().equals(Tc.EXPIRED)) {
					throw (new SvException("naits.info.exportCertAlreadyExpired", svr.getInstanceUser()));
				}
				if (dboQuarantine != null && dboQuarantine.getVal(Tc.DATE_TO) != null) {
					if (vc.checkIfDateOfInsertIsBeforeEndDate(dboQuarantine, svr)) {
						throw (new SvException("naits.error.animalCantBeCertifiedBeforeEndDateOfQuarantine",
								svCONST.systemUser, null, null));
					}
					DateTime dtDateTo = new DateTime(dboQuarantine.getVal(Tc.DATE_TO).toString());
					DateTime dtNow = new DateTime();
					Long numDaysAfterQuarantineExpired = ValidationChecks.getDateDiff(dtNow, dtDateTo, TimeUnit.DAYS);
					if (numDaysAfterQuarantineExpired > 1) {
						throw (new SvException("naits.error.animalsCanBeCertified24HoursAfterEndDateOfQuarantine",
								svCONST.systemUser, null, null));
					}
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					if (!dboToHandleStatus.equals(Tc.EMPTY_STRING) && dboToHandleStatus.equals(Tc.PENDING_EX)) {
						dboObjectToHandle.setStatus(Tc.EXPORTED);
						svw.saveObject(dboObjectToHandle, false);
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			DbDataObject linkAnimalExportCert = SvReader.getLinkType(Tc.ANIMAL_EXPORT_CERT,
					SvReader.getTypeIdByName(Tc.ANIMAL), SvReader.getTypeIdByName(Tc.EXPORT_CERT));
			DbDataArray exportedAnimals = svr.getObjectsByLinkedId(exportCertObjectId,
					SvReader.getTypeIdByName(Tc.EXPORT_CERT), linkAnimalExportCert, SvReader.getTypeIdByName(Tc.ANIMAL),
					true, null, 0, 0);
			boolean isProcessedQuarantine = true;
			for (DbDataObject animalObj : exportedAnimals.getItems()) {
				if (animalObj != null && !animalObj.getStatus().equals(Tc.EXPORTED)) {
					isProcessedQuarantine = false;
					break;
				}
			}
			if (isProcessedQuarantine) {
				sww.moveObject(dboExportCertificate, Tc.PROCESSED, false);
				resultMessage = "naits.success.exportAnimalStatusProcessed";
			}
			if (dboExportCertificate != null) {
				@SuppressWarnings(Tc.UNUSED)
				DbDataObject refreshExportCertCache = svr.getObjectById(exportCertObjectId,
						dboExportCertificate.getObject_type(), new DateTime());
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
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
		return resultMessage;
	}

	public String updateStatusOfMovementDoc(JsonArray jsonData, String nextStatus, SvReader svr) throws SvException {
		String result = "naits.error.errorMovementDocStatusUpdate";
		SvWorkflow sww = null;
		SvWriter svw = null;
		Reader rdr = new Reader();
		Writer wr = new Writer();
		DbDataArray animalOrFlockMovements = null;
		String mvmDocType = Tc.EMPTY_STRING;
		try {
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			svr.setAutoCommit(false);

			String action = nextStatus.toUpperCase().trim();
			DbDataObject movementDocTypeDesc = SvReader.getDbtByName(Tc.MOVEMENT_DOC);

			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("MOVEMENT_DOC.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("MOVEMENT_DOC.OBJECT_ID").getAsLong(),
							movementDocTypeDesc, null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_movement_found", svr.getInstanceUser()));
				}
				animalOrFlockMovements = rdr.getAnimalMovementsByMovementDoc(dboObjectToHandle, false, svr);
				if (animalOrFlockMovements == null || animalOrFlockMovements.size() < 1) {
					throw (new SvException("naits.error.mvmDocIsEmpty", svr.getInstanceUser()));
				}
				if (dboObjectToHandle.getVal(Tc.MOVEMENT_TYPE) != null) {
					mvmDocType = dboObjectToHandle.getVal(Tc.MOVEMENT_TYPE).toString().toUpperCase();
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					switch (action) {
					case Tc.RELEASED:
						Boolean resolvedFlag = true;
						result = "naits.error.mvmDocStatusHasToBeDraft";
						if (dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.DRAFT)) {
							result = "naits.success.releasedMovementDoc";
							DbDataArray movementDocBlocksArr = svr.getObjectsByParentId(
									dboObjectToHandle.getObject_id(), SvReader.getTypeIdByName(Tc.MOVEMENT_DOC_BLOCK),
									null, 0, 0);

							if (movementDocBlocksArr != null && !movementDocBlocksArr.getItems().isEmpty()) {
								for (DbDataObject mvmDocBlockObj : movementDocBlocksArr.getItems()) {
									if (mvmDocBlockObj.getStatus().equals(Tc.UNRESOLVED)) {
										resolvedFlag = false;
										break;
									}
								}
								result = "naits.error.mvmDocHaveMvmDocBlocked";
								if (resolvedFlag.equals(true)) {
									sww.moveObject(dboObjectToHandle, Tc.RELEASED, false);
									result = "naits.success.releasedMovementDoc";
								}
							} else {
								sww.moveObject(dboObjectToHandle, Tc.RELEASED, false);
							}
							if (dboObjectToHandle.getStatus().equals(Tc.RELEASED)) {
								for (DbDataObject animalOrFlockMovementObj : animalOrFlockMovements.getItems()) {
									DbDataObject dboAnimalOrFlock = svr.getObjectById(
											animalOrFlockMovementObj.getParent_id(),
											SvReader.getTypeIdByName(mvmDocType), new DateTime());
									DbDataObject dboDestinationHolding = rdr.findAppropriateHoldingByPic(
											animalOrFlockMovementObj.getVal(Tc.DESTINATION_HOLDING_ID).toString(), svr);
									wr.finishAnimalOrFlockMovement(dboAnimalOrFlock, dboDestinationHolding,
											new DateTime().toString().substring(0, 10), null, null, true, svw, sww);
								}
								wr.autoAsignSessionUserToObjectField(dboObjectToHandle, Tc.RECIPIENT_USER, false, svr);
								svw.saveObject(dboObjectToHandle, false);
							}
						}
						break;
					case "CANCELLED":
						if (dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.DRAFT)) {
							for (DbDataObject movementObj : animalOrFlockMovements.getItems()) {
								DbDataObject animalOrFlockObj = svr.getObjectById(movementObj.getParent_id(),
										SvReader.getTypeIdByName(mvmDocType), null);
								if (animalOrFlockObj != null) {
									wr.cancelMovement(animalOrFlockObj, sww, svr);
								}
							}
							result = "naits.success.canceledMovementDoc";
							sww.moveObject(dboObjectToHandle, Tc.CANCELED, false);
						} else {
							result = "naits.error.mvmDocStatusHasToBeDraft";
						}
						break;
					default:
						break;
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svr.dbCommit();
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage());
		} finally {
			if (svw != null) {
				svw.release();
			}
			if (sww != null) {
				sww.release();
			}
		}
		return result;
	}

	/**
	 * Method that checks Animal Movements in Movement Document. If some of the
	 * animals/flock in movement have sign of disease, Movement Document Block is
	 * generated. Initial status of Movement Document Block is "UNRESOLVED". If all
	 * of the blocking reasons are resolved, and we check the document again, status
	 * of the Movement Document Block will change into "RESOLVED"
	 * 
	 * @param dboMovementDoc
	 * @param sessionId
	 * @return
	 * @throws Exception
	 */
	public String checkAnimalOrFlockMovementsInMovementDocument(DbDataObject dboMovementDoc, String sessionId)
			throws SvException {
		String resultMessage = Tc.successCheckMovementsInMvmDoc;
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Reader rdr = null;
		Writer wr = null;
		DbDataArray animalOrFlockMovements = null;
		String tableName = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);

			svw = new SvWriter(svr);
			sww = new SvWorkflow(svr);

			svw.setAutoCommit(false);
			sww.setAutoCommit(false);

			rdr = new Reader();
			wr = new Writer();
			int counter = 0;

			Long movementDocObjectId = dboMovementDoc.getObject_id();

			if (dboMovementDoc != null) {
				animalOrFlockMovements = rdr.getAnimalMovementsByMovementDoc(dboMovementDoc, false, svr);

				if (animalOrFlockMovements == null || animalOrFlockMovements.size() < 1) {
					throw (new SvException("naits.error.noMovementFound", svr.getInstanceUser()));
				}

				if (dboMovementDoc.getVal(Tc.MOVEMENT_TYPE) != null) {
					tableName = dboMovementDoc.getVal(Tc.MOVEMENT_TYPE).toString();
				}

				for (DbDataObject animalOrFlockMovementObj : animalOrFlockMovements.getItems()) {
					// checks
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(animalOrFlockMovementObj.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
						}
						DbDataObject existingMovementDocBlockObject = rdr.getMovementDocBlockByMovementObjId(
								movementDocObjectId, animalOrFlockMovementObj.getObject_id(), svr);
						DbDataObject mvmDocBlockObj = movementDocProhibitionChecks(animalOrFlockMovementObj,
								movementDocObjectId, tableName, rdr, wr, svr);
						if (mvmDocBlockObj != null && existingMovementDocBlockObject == null) {
							resultMessage = "naits.info.checkMovementsInNewMvmDocGeneratedBlock";
							svw.saveObject(mvmDocBlockObj, false);
						} else if (mvmDocBlockObj != null && existingMovementDocBlockObject != null) {
							resultMessage = "naits.info.successCheckMovementsInMvmDocGeneratedBlock";
						} else if (mvmDocBlockObj == null && existingMovementDocBlockObject != null) {
							// if there are no more blocks, change the status of
							// the
							// existing one
							if (existingMovementDocBlockObject.getStatus().equals(Tc.UNRESOLVED)) {
								resultMessage = Tc.successCheckMovementsInMvmDoc;
								sww.moveObject(existingMovementDocBlockObject, Tc.RESOLVED, false);
							}
						}
						if (existingMovementDocBlockObject != null
								&& existingMovementDocBlockObject.getStatus().equals(Tc.RESOLVED)) {
							counter++;
							if (counter == animalOrFlockMovements.size()) {
								sww.moveObject(dboMovementDoc, Tc.DRAFT, false);
							}
						}
					} finally {
						if (lock != null && animalOrFlockMovementObj != null) {
							SvLock.releaseLock(String.valueOf(animalOrFlockMovementObj.getObject_id()), lock);
						}
					}
				}
			}
			svw.dbCommit();
			sww.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
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
		return resultMessage;
	}

	public DbDataObject movementDocProhibitionChecks(DbDataObject movementObj, Long movementDocObjectId,
			String tableName, Reader rdr, Writer wr, SvReader svr) throws SvException {
		DbDataObject mvmDocBlockObj = null;

		ArrayList<String> blockReasons = new ArrayList<>();
		ArrayList<String> recommendBlck = new ArrayList<>();

		DbDataObject dboMovementDoc = svr.getObjectById(movementDocObjectId, SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
				null);
		String tempBlockReason = Tc.EMPTY_STRING;
		String diseaseCausePerLabSample = Tc.EMPTY_STRING;

		DbDataObject animalOrFlockObj = svr.getObjectById(movementObj.getParent_id(),
				SvReader.getTypeIdByName(tableName), null);
		DbDataObject labSample = null;
		DbDataObject diseaseObj = null;
		Integer daysDifference = null;
		if (animalOrFlockObj != null) {
			DbDataObject sourceHoldingObj = svr.getObjectById(animalOrFlockObj.getParent_id(),
					SvReader.getTypeIdByName(Tc.HOLDING), null);

			// First check if there is Laboratory Sample with
			// positive
			// or inconclusive status per animal
			if (!tableName.equals(Tc.FLOCK)) {
				tempBlockReason = rdr.getHealthStatusPerAnimalAccordingLabSample(animalOrFlockObj, svr);
				if (!tempBlockReason.equals("")) {
					String[] blockingObjIdAndDiseaseCause = tempBlockReason.trim().split(",");
					Long objIdOfBlockingLabSample = Long.valueOf(blockingObjIdAndDiseaseCause[0].trim());
					diseaseCausePerLabSample = blockingObjIdAndDiseaseCause[1].trim();
					labSample = svr.getObjectById(objIdOfBlockingLabSample, SvReader.getTypeIdByName(Tc.LAB_SAMPLE),
							null);
					// in this case tempBlockReason returns code
					if (diseaseCausePerLabSample.endsWith("S")) {
						// check if inconclusive (SUSPECT) animals are tested
						// again
						if (!rdr.checkifInconclusiveSamplesAreTestedAgainPerAnimal(animalOrFlockObj, svr)) {
							blockReasons.add(diseaseCausePerLabSample);
							recommendBlck.add(diseaseCausePerLabSample + "x");
						}
					} else {
						// if status is positive/inconclusive
						blockReasons.add(diseaseCausePerLabSample);
						recommendBlck.add(diseaseCausePerLabSample + "x");
					}

					if (labSample != null && labSample.getVal(Tc.DATE_OF_COLLECTION) != null) {
						daysDifference = (int) ValidationChecks.getDateDiff(
								new DateTime(dboMovementDoc.getVal("DT_REGISTRATION").toString()),
								new DateTime(labSample.getVal(Tc.DATE_OF_COLLECTION).toString()), TimeUnit.DAYS);
					}
					// this check is if animal has suspect sample or positive
					// sample
					if (blockReasons != null && !blockReasons.isEmpty()) {
						if (diseaseCausePerLabSample.equalsIgnoreCase(Tc.RABIES_POSITIVE)
								|| diseaseCausePerLabSample.equalsIgnoreCase(Tc.RABIES_SUSPECT)) {
							// RABIES
							diseaseObj = rdr.getDiseaseObjByDiseaseName("5", svr);
							Boolean allAnimalsVaccinatedBetweenPeriod = rdr
									.checkIfAnimalsHasValidImmunizationPeriodInAnimalHealthBook(sourceHoldingObj,
											labSample.getVal(Tc.DATE_OF_COLLECTION).toString(), 10, svr);

							// CHECK IF ALL ANIMALS IN HOLDING ARE VACCINATED
							if (allAnimalsVaccinatedBetweenPeriod != true) {
								blockReasons.add(Tc.ALL_ANIMALS_VACC_PERIOD_10);
								recommendBlck.add(Tc.VACC_ALL_ANIMALS);
								recommendBlck.add(Tc.DISEASE_PROHIBIT_VACC_PERIOD_ARB);
							}

							// CHECK IF MOVEMENT IS STILL BLOCKED
							if (daysDifference != null && daysDifference <= Integer
									.valueOf(diseaseObj.getVal(Tc.IMMUNIZATION_PERIOD).toString())) {
								recommendBlck.add(Tc.PROHIBIT_PERIOD_RABIES);
							}
						} else if (diseaseCausePerLabSample.equalsIgnoreCase(Tc.ANTHRAX_POSITIVE)
								|| diseaseCausePerLabSample.equalsIgnoreCase(Tc.ANTHRAX_SUSPECT)) {
							// ANTHRAX
							diseaseObj = rdr.getDiseaseObjByDiseaseName("2", svr);
							boolean allAnimalsVaccinatedBetweenPeriod = rdr
									.checkIfAnimalsHasValidImmunizationPeriodInAnimalHealthBook(sourceHoldingObj,
											labSample.getVal(Tc.DATE_OF_COLLECTION).toString(), 10, svr);

							// CHECK IF ALL ANIMALS IN HOLDING ARE VACCINATED
							if (!allAnimalsVaccinatedBetweenPeriod) {
								blockReasons.add(Tc.ALL_ANIMALS_VACC_PERIOD_10);
								recommendBlck.add(Tc.VACC_ALL_ANIMALS);
								recommendBlck.add(Tc.DISEASE_PROHIBIT_VACC_PERIOD_ARB);
							}
							// CHECK IF MOVEMENT IS STILL BLOCKED / IMM_PERIOD
							if (daysDifference != null && daysDifference <= Integer
									.valueOf(diseaseObj.getVal(Tc.IMMUNIZATION_PERIOD).toString())) {
								recommendBlck.add(Tc.PROHIBIT_PERIOD_ANTHRAX);
							}
						} else if (diseaseCausePerLabSample.equalsIgnoreCase(Tc.BRUC_POSITIVE)
								|| diseaseCausePerLabSample.equalsIgnoreCase(Tc.BRUC_SUSPECT)) {
							// BRUCELLOSIS
							Boolean allAnimalsVaccinatedBetweenPeriod = rdr
									.checkIfAnimalsHasValidImmunizationPeriodInAnimalHealthBook(sourceHoldingObj,
											labSample.getVal(Tc.DATE_OF_COLLECTION).toString(), 10, svr);

							// CHECK IF ALL ANIMALS IN HOLDING ARE VACCINATED
							if (!allAnimalsVaccinatedBetweenPeriod) {
								blockReasons.add(Tc.ALL_ANIMALS_VACC_PERIOD_10);
								recommendBlck.add(Tc.VACC_ALL_ANIMALS);
								recommendBlck.add(Tc.DISEASE_PROHIBIT_VACC_PERIOD_ARB);
							}

						} else if (diseaseCausePerLabSample.equalsIgnoreCase(Tc.FMD_POSITIVE)
								|| diseaseCausePerLabSample.equalsIgnoreCase(Tc.FMD_SUSPECT)) {
							// FOOT AND MOUTH DISEASE
							diseaseObj = rdr.getDiseaseObjByDiseaseName("1", svr);
							boolean allAnimalsVaccinatedBetweenPeriod = rdr
									.checkIfAnimalsHasValidImmunizationPeriodInAnimalHealthBook(sourceHoldingObj,
											labSample.getVal(Tc.DATE_OF_COLLECTION).toString(), 6, svr);

							// CHECK IF ALL ANIMALS IN HOLDING ARE VACCINATED
							if (!allAnimalsVaccinatedBetweenPeriod) {
								blockReasons.add(Tc.ALL_ANIMALS_VACC_PERIOD_6);
								recommendBlck.add(Tc.VACC_ALL_ANIMALS);
								recommendBlck.add(Tc.DISEASE_PROHIBIT_VACC_PERIOD_FMD);
							}

							// CHECK IF MOVEMENT IS STILL BLOCKED / IMM_PERIOD
							if (daysDifference != null && daysDifference <= Integer
									.valueOf(diseaseObj.getVal(Tc.IMMUNIZATION_PERIOD).toString())) {
								recommendBlck.add(Tc.PROHIBIT_PERIOD_FOOT_AND_MOUTH);// CHANGE
							}

						} else if (diseaseCausePerLabSample.equalsIgnoreCase(Tc.TUBER_POSITIVE)
								|| diseaseCausePerLabSample.equalsIgnoreCase(Tc.TUBER_SUSPECT)) {
							// TUBERCULLOSIS
							recommendBlck.add(Tc.VACC_ALL_ANIMALS);
						}
					}
				}
			}
			// Epidemiological check Source-Destination holding
			if (sourceHoldingObj != null && movementObj.getVal(Tc.DESTINATION_HOLDING_ID) != null) {
				rdr.checkifMovementIsAllowedDependOnAreaHealthStatus(sourceHoldingObj, tempBlockReason, blockReasons,
						recommendBlck, animalOrFlockObj, movementObj, svr);
			}
		}
		// For MOVEMENT_DOC_BLOCK
		if ((blockReasons != null && !blockReasons.isEmpty()) || (recommendBlck != null && !recommendBlck.isEmpty())) {
			String blckRsn = blockReasons.toString().replace("[", "").replace("]", "").replace(" ", "");
			String recomm = recommendBlck.toString().replace("[", "").replace("]", "").replace(" ", "");
			mvmDocBlockObj = wr.createMovementDocBlock(movementDocObjectId, movementObj, blckRsn, recomm, svr);
			if ((blockReasons.size() == 1 && (blockReasons.contains(Tc.SOURCE_HOLDING_AFFECTED_AREA)
					|| blockReasons.contains(Tc.DESTINATION_HOLDING_AFFECTED_AREA)))
					|| (blockReasons.size() == 2 && blockReasons.contains(Tc.SOURCE_HOLDING_AFFECTED_AREA)
							&& blockReasons.contains(Tc.DESTINATION_HOLDING_AFFECTED_AREA))) {
				mvmDocBlockObj = null;
			}

		}
		return mvmDocBlockObj;
	}

	public String createActivityPeriod(Long holdingObjId, String dateFrom, String dateTo, JsonArray jsonData,
			String sessionId) {
		String resultMessage = "naits.success.createActivityPeriod";
		SvReader svr = null;
		SvParameter svp = null;
		Reader rdr = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svp = new SvParameter(svr);
			rdr = new Reader();
			wr = new Writer();

			svp.setAutoCommit(false);
			DbDataObject holdingResponsibleTypeDesc = SvReader.getDbtByName(Tc.HOLDING_RESPONSIBLE);
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;

				if (obj.get("HOLDING_RESPONSIBLE.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("HOLDING_RESPONSIBLE.OBJECT_ID").getAsLong(),
							holdingResponsibleTypeDesc, null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_person_found", svr.getInstanceUser()));
				}
				if (dateFrom == null || dateFrom.equals("")) {
					throw (new SvException("naits.error.dateFromIsMissing", svr.getInstanceUser()));
				}
				if (dateTo == null || dateTo.equals("")) {
					throw (new SvException("naits.error.dateToIsMissing", svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					DbDataObject linkBetweenHerderAndHolding = SvReader.getLinkType(Tc.HOLDING_HERDER,
							SvReader.getTypeIdByName(Tc.HOLDING), SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE));
					DbDataObject dboLink = rdr.getLinkObject(holdingObjId, dboObjectToHandle.getObject_id(),
							linkBetweenHerderAndHolding.getObject_id(), svr);
					if (dboLink != null) {
						wr.createActivityPeriodPerHerder(dateFrom, dateTo, dboLink, svp);
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svp.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svp != null) {
				svp.release();
			}
		}
		return resultMessage;
	}

	public String updateStatusMassAction(String tableName, String nextStatus, JsonArray jsonData, String sessionId) {
		String resultMessage = "naits.success.massTransferAction";
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);
			rdr = new Reader();

			DbDataObject dbObjectType = SvReader.getDbtByName(tableName);
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataArray arrDbObjectsToSave = new DbDataArray();
			int count = 0;
			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get(tableName + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(tableName + "." + Tc.OBJECT_ID).getAsLong(),
							dbObjectType, new DateTime());
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error." + tableName + "_missing", svr.getInstanceUser()));
				}
				if (tableName.equals(Tc.TRANSFER) && !rdr.hasPermission(tableName, Tc.FULL, svr)) {
					throw (new SvException("naits.error.userNotAuthorisedToPerformFollowingAction",
							svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					switch (tableName) {
					case Tc.TRANSFER:
						if (!dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.DRAFT)
								&& (nextStatus.equalsIgnoreCase(Tc.CANCELED)
										|| nextStatus.equalsIgnoreCase(Tc.DELIVERED))) {
							throw (new SvException("naits.error.onlyDraftTransferCanBeCanceled",
									svr.getInstanceUser()));
						}
						if (nextStatus.equalsIgnoreCase(Tc.DELIVERED)) {
							dboObjectToHandle.setVal(Tc.RECEIVED_BY, dboUser.getVal(Tc.USER_NAME).toString());
							dboObjectToHandle.setVal(Tc.DATE_RECEIVED, new DateTime());
						}
						dboObjectToHandle.setStatus(nextStatus.toUpperCase());
						arrDbObjectsToSave.addDataItem(dboObjectToHandle);
						count++;
						if (count == COMMIT_COUNT) {
							svw.saveObject(arrDbObjectsToSave, true, true);
							arrDbObjectsToSave = new DbDataArray();
							count = 0;
						}
						break;
					default:
						sww.moveObject(dboObjectToHandle, nextStatus.toUpperCase(), true);
						break;
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			if (!arrDbObjectsToSave.getItems().isEmpty()) {
				svw.saveObject(arrDbObjectsToSave, true, true);
			}
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
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
		return resultMessage;
	}

	public String assignOrUnassignNotification(String userOrGroupName, String actionType, JsonArray jsonData,
			String sessionId) {
		String resultMessage = "naits.success.successfullyAssignedNotification";
		SvReader svr = null;
		SvWriter svw = null;
		SvNotification svn = null;
		Reader rdr = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svn = new SvNotification(svw);
			svw.setAutoCommit(false);
			rdr = new Reader();
			wr = new Writer();

			String paramActionType = actionType.toUpperCase();
			String objectType = Tc.USER;
			String linkName = Tc.LINK_NOTIFICATION_USER;

			Long objectTypeDec = svCONST.OBJECT_TYPE_NOTIFICATION;
			DbDataObject dboUserOrUserGroup = rdr.searchForObject(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME,
					userOrGroupName, svr);

			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("SVAROG_NOTIFICATION.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_NOTIFICATION.OBJECT_ID").getAsLong(),
							objectTypeDec, null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.notification_missing", svr.getInstanceUser()));
				}
				if (dboUserOrUserGroup == null) {
					dboUserOrUserGroup = rdr.searchForObject(svCONST.OBJECT_TYPE_GROUP, Tc.GROUP_NAME, userOrGroupName,
							svr);
					objectType = Tc.USER_GROUP;
					linkName = Tc.LINK_NOTIFICATION_GROUP;
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					if (dboUserOrUserGroup != null) {
						// get link between notification and user or user_group
						DbDataObject dboLinkBetweenNotificationAndUser = SvLink.getLinkType(linkName,
								svCONST.OBJECT_TYPE_NOTIFICATION, dboUserOrUserGroup.getObject_type());
						DbDataObject dboLink = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
								dboUserOrUserGroup.getObject_id(), dboLinkBetweenNotificationAndUser.getObject_id(),
								svr);

						if (paramActionType.equals(Tc.ASSIGN)) {
							if (dboLink == null) {
								svn.createLinkBetweenNotificationAndUserOrUserGroup(dboObjectToHandle,
										dboUserOrUserGroup, true);
								resultMessage += objectType;
							} else {
								resultMessage = "naits.error.selectedNotificationAlreadyAssignedToSelected"
										+ objectType;
							}

						} else if (paramActionType.equals(Tc.UNASSIGN)) {
							if (dboLink != null) {
								wr.invalidateLink(dboLink, true, svr);
								resultMessage = "naits.success.successfullyUnassignedNotification" + objectType;
							} else {
								resultMessage = "naits.error.selectedNotificationIsNotAssignedToSelected" + objectType;
							}
						}
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svw.dbCommit();
			svn.dbCommit();
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			resultMessage = sve.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svn != null) {
				svn.release();
			}
		}
		return resultMessage;
	}

	/**
	 * Method for executing mass action on objects. Will be extended according
	 * needs.
	 * 
	 * @param tableName
	 * @param actionName
	 * @param subActionName
	 * @param objectId
	 * @param jsonData
	 * @param sessionId
	 * @return
	 * @throws Exception
	 */
	public String inventoryIndividualDirectTransfer(Long destinationObjId, JsonArray jsonData, String sessionId)
			throws Exception {
		String result = "naits.success." + Tc.INVENTORY_ITEM + "MassAction";
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		Writer wr = null;
		ValidationChecks vc = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			wr = new Writer();
			rdr = new Reader();
			vc = new ValidationChecks();

			if (jsonData.size() > 100) {
				throw (new SvException("naits.error.individualTransferNumberIsOverLimit", svr.getInstanceUser()));
			}

			DbDataArray arrDbObjectsToSave = new DbDataArray();

			for (JsonElement jsonElement : jsonData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				DbDataObject dboParentObjectToHandle = null;
				if (obj.get(Tc.INVENTORY_ITEM + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(Tc.INVENTORY_ITEM + "." + Tc.OBJECT_ID).getAsLong(),
							SvReader.getTypeIdByName(Tc.INVENTORY_ITEM), null);
				}
				if (obj.get(Tc.INVENTORY_ITEM + "." + Tc.PARENT_ID) != null) {
					dboParentObjectToHandle = svr.getObjectById(
							obj.get(Tc.INVENTORY_ITEM + "." + Tc.PARENT_ID).getAsLong(), svCONST.OBJECT_TYPE_ORG_UNITS,
							null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error." + Tc.INVENTORY_ITEM + "_missing", svr.getInstanceUser()));
				}
				if (dboParentObjectToHandle == null) {
					dboParentObjectToHandle = svr.getObjectById(
							obj.get(Tc.INVENTORY_ITEM + "." + Tc.PARENT_ID).getAsLong(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
				}
				if (dboParentObjectToHandle == null) {
					throw (new SvException("naits.error." + Tc.INVENTORY_ITEM + "_missing", svr.getInstanceUser()));
				}
				DbDataObject dboTransfer = null;

				String tagType = dboObjectToHandle.getVal(Tc.TAG_TYPE).toString();
				Long startTagId = Long.valueOf(dboObjectToHandle.getVal(Tc.EAR_TAG_NUMBER).toString());
				Long endTagId = Long.valueOf(dboObjectToHandle.getVal(Tc.EAR_TAG_NUMBER).toString());
				result = "naits.error.arrivalPlaceNotFound";
				DbDataObject dboDestinationOrgUnit = svr.getObjectById(Long.valueOf(destinationObjId),
						svCONST.OBJECT_TYPE_ORG_UNITS, null);
				if (dboDestinationOrgUnit == null) {
					dboDestinationOrgUnit = svr.getObjectById(Long.valueOf(destinationObjId),
							SvReader.getTypeIdByName(Tc.HOLDING), null);
				}
				if (dboDestinationOrgUnit != null && dboParentObjectToHandle != null) {
					if (dboDestinationOrgUnit.getObject_id().equals(dboParentObjectToHandle.getObject_id())) {
						throw (new SvException("naits.error.cantSendInventoryItemsToSameOrgUnit", svCONST.systemUser,
								null, null));
					}
					String destinationName = dboDestinationOrgUnit.getVal(Tc.NAME).toString();
					if (dboDestinationOrgUnit.getObject_type().equals(SvReader.getTypeIdByName(Tc.HOLDING))) {
						destinationName = dboDestinationOrgUnit.getVal(Tc.PIC).toString();
					}
					dboTransfer = wr.createTransferObject(dboObjectToHandle.getVal(Tc.TAG_TYPE).toString(), startTagId,
							endTagId, null, null, dboParentObjectToHandle.getVal(Tc.NAME).toString(), destinationName,
							null, null, null, String.valueOf(dboParentObjectToHandle.getObject_id()),
							String.valueOf(dboDestinationOrgUnit.getObject_id()));
					dboTransfer.setParent_id(dboParentObjectToHandle.getObject_id());
					dboTransfer.setVal(Tc.DIRECT_TRANSFER, true);
					dboTransfer.setVal(Tc.CACHE_PARENT_ID, dboParentObjectToHandle.getObject_id());
					dboTransfer.setVal(Tc.TRANSFER_TYPE, Tc.DIRECT_TRANSFER);
					if (vc.checkIfTransferIsOverlapping(dboTransfer.getObject_type(),
							dboParentObjectToHandle.getObject_id(), tagType, startTagId, endTagId, rdr, svr)) {
						throw new SvException("naits.error.beforeSaveCheck_range_overlapping_transfer",
								svr.getInstanceUser());
					}
					arrDbObjectsToSave.addDataItem(dboTransfer);
				}
			}
			// expected max 10 transfer in one call
			if (!arrDbObjectsToSave.getItems().isEmpty()) {
				svw.saveObject(arrDbObjectsToSave, true, true);
			}
			for (DbDataObject tempTransfer : arrDbObjectsToSave.getItems()) {
				wr.directInventoryTransfer(tempTransfer, false, svr, svw);
			}
			result = "naits.success.moveInventoryItemToOtherOrgUnit";
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			result = sve.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return result;
	}

	public String inventoryDirectTransfer(Long transferObjId, String sessionId) throws Exception {
		String result = "naits.success.inventoryItemDirectTransfer";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			DbDataObject dboTransfer = svr.getObjectById(transferObjId, SvReader.getTypeIdByName(Tc.TRANSFER), null);
			result = wr.directInventoryTransfer(dboTransfer, true, svr, svw);
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			result = sve.getLabelCode();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return result;
	}

	/**
	 * @param jsonData
	 * @param sessionId
	 * @return
	 * @throws Exception
	 */

	public String massObjectHandler(JsonObject jsonData, String sessionId) throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvGeometry svg = null;
		ValidationChecks vc = null;
		Reader rdr = null;
		Writer wr = null;
		String resultMessage = "naits.success.petMassAction";
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);

			svg = new SvGeometry(svw);
			svg.setAllowNullGeometry(true);
			svg.setAutoCommit(false);

			// MASS ACTION PARAMETERS
			String tableName = null;
			String actionName = null;
			String subActionName = null;
			String actionParam = null;
			String actionDate = null;

			DbDataObject dboVaccinationEvent = null;

			String regex = Tc.NUMBER_REGEX;

			vc = new ValidationChecks();
			rdr = new Reader();
			wr = new Writer();

			JsonArray jsonArrayData = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
			JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();
			JsonObject petLocationAdditionalData = null;

			// set parameters
			for (JsonElement jsonElement : jsonParams) {
				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_TBL_NAME)) {
					tableName = obj.get(Tc.MASS_PARAM_TBL_NAME).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ACTION)) {
					actionName = obj.get(Tc.MASS_PARAM_ACTION).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_SUBACTION)) {
					subActionName = obj.get(Tc.MASS_PARAM_SUBACTION).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ADDITIONAL_PARAM)) {
					actionParam = obj.get(Tc.MASS_PARAM_ADDITIONAL_PARAM).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ACTION_DATE)) {
					actionDate = obj.get(Tc.MASS_PARAM_ACTION_DATE).getAsString();
				}
				if (obj.has(Tc.PET_LOCATION_DETAILS)) {
					petLocationAdditionalData = obj.get(Tc.PET_LOCATION_DETAILS).getAsJsonObject();
				}
			}

			DbDataObject dboTypeDesc = SvReader.getDbt(SvReader.getTypeIdByName(tableName));
			DbDataObject dboHoldingTypeDesc = SvReader.getDbt(SvReader.getTypeIdByName(Tc.HOLDING));
			DbDataObject currentDboUser = SvReader.getUserBySession(sessionId);

			// selected row elements
			for (JsonElement jsonElement : jsonArrayData) {
				JsonObject obj = jsonElement.getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				DbDataObject dboParentObjectToHandle = null;

				if (obj.has(tableName + "." + Tc.OBJECT_ID)) {
					dboObjectToHandle = svr.getObjectById(obj.get(tableName + "." + Tc.OBJECT_ID).getAsLong(),
							dboTypeDesc, null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.no_" + tableName.toLowerCase() + "_found",
							svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					}
					switch (tableName) {
					case Tc.PET:
						if (obj.has(tableName + "." + Tc.PARENT_ID)) {
							dboParentObjectToHandle = svr.getObjectById(
									obj.get(tableName + "." + Tc.PARENT_ID).getAsLong(), dboHoldingTypeDesc, null);
						}
						if (dboObjectToHandle.getVal(Tc.PET_TAG_ID) == null
								&& !Tc.DIED_EUTHANASIA.equals(subActionName)) {
							throw (new SvException("naits.error.onlyEuthanizeActionCanBeExecutedOnPetsWithoutId",
									svr.getInstanceUser()));
						}
						switch (actionName) {
						case Tc.ACTIVITY:
							DbDataObject dboPetHealthBook = null;
							if (actionParam != null && actionParam.matches(regex)) {
								dboVaccinationEvent = svr.getObjectById(Long.valueOf(actionParam),
										SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);
							}
							switch (subActionName) {
							case Tc.CAMPAIGN:
								if (dboVaccinationEvent != null) {
									List<String> animalTypes = rdr.getMultiSelectFieldValueAsList(dboVaccinationEvent,
											Tc.ANIMAL_TYPE);
									if (!vc.checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(animalTypes,
											dboObjectToHandle)) {
										throw (new SvException("naits.error.petCampaignNotApplicableOnSelectedObjects",
												svr.getInstanceUser()));
									}
									dboPetHealthBook = wr.createOrUpdatePetHealthBookAccordingVaccinationEvent(null,
											dboVaccinationEvent, actionDate,
											currentDboUser.getVal(Tc.USER_NAME).toString(),
											dboObjectToHandle.getObject_id());
									svw.saveObject(dboPetHealthBook, false);
								}
								break;
							case Tc.VACCINATION:
								dboPetHealthBook = wr.createPetHealthBook(actionDate, "1",
										dboObjectToHandle.getObject_id());
								svw.saveObject(dboPetHealthBook, false);
								break;
							case Tc.SAMPLING:
								dboPetHealthBook = wr.createPetHealthBook(actionDate, "2",
										dboObjectToHandle.getObject_id());
								svw.saveObject(dboPetHealthBook, false);
								break;
							case Tc.DISINFECTION:
								break;
							case Tc.CASTRATION:
								dboObjectToHandle.setVal(Tc.DT_CASTRATION, new DateTime(actionDate));
								dboObjectToHandle.setVal(Tc.IS_CASTRATED, "1");
								svw.saveObject(dboObjectToHandle, false);
								break;
							default:
								break;
							}
							break;
						case Tc.UPDATE_STATUS:
							if (new DateTime(actionDate).isAfter(new DateTime())) {
								throw (new SvException("naits.error.actionDateCannotBeInTheFuture",
										svr.getInstanceUser()));
							}
							switch (subActionName) {
							case Tc.RELEASED:
								dboObjectToHandle.setStatus(subActionName);
								String sourceArchiveNumber = wr.generateArchiveNumber(dboObjectToHandle,
										dboParentObjectToHandle, Tc.RELEASE_EVENT, svr);
								wr.createPetMovement(dboObjectToHandle, null, null, new DateTime(actionDate),
										Tc.RELEASED, currentDboUser.getVal(Tc.USER_NAME).toString(), Tc.RELEASE,
										sourceArchiveNumber, null, false, svw, svr);
								break;
							case Tc.DIED:
								dboObjectToHandle.setStatus(subActionName);
								dboObjectToHandle.setVal(Tc.DEATH_DATE, new DateTime(actionDate));
								break;
							case Tc.DIED_EUTHANASIA:
								dboObjectToHandle.setStatus(Tc.EUTHANIZED);
								dboObjectToHandle.setVal(Tc.DEATH_DATE, new DateTime(actionDate));
								dboObjectToHandle.setVal(Tc.DT_EUTHANASIA, new DateTime(actionDate));
								break;
							case Tc.EXPORTED:
								dboObjectToHandle.setStatus(Tc.EXPORTED);
								break;
							case Tc.INACTIVE:
								dboObjectToHandle.setStatus(Tc.INACTIVE);
								break;
							default:
								break;
							}
							if (dboObjectToHandle.getIs_dirty()) {
								svw.saveObject(dboObjectToHandle, false);
							}
							break;
						case Tc.CREATE_MOVEMENT:
							DateTime dateOfMovement = new DateTime(actionDate);
							if (dateOfMovement.isAfter(new DateTime())) {
								throw (new SvException("naits.error.activityDateCannotBeInTheFuture",
										svr.getInstanceUser()));
							}
							DbDataObject dboDestinationHolding = svr.getObjectById(Long.valueOf(actionParam),
									SvReader.getTypeIdByName(Tc.HOLDING), null);
							if (dboDestinationHolding == null || dboDestinationHolding.getVal(Tc.PIC) == null) {
								throw (new SvException("naits.error.invalidDestinationHolding", svr.getInstanceUser()));
							}
							if (dboParentObjectToHandle.getObject_id().equals(dboDestinationHolding.getObject_id())) {
								throw (new SvException("naits.error.sourceAndDestinationShelterAreSame",
										svr.getInstanceUser()));
							}
							String sourceArchiveNumber = wr.generateArchiveNumber(dboObjectToHandle,
									dboParentObjectToHandle, Tc.RELEASE_EVENT, svr);
							String destinationArchiveNumber = wr.generateArchiveNumber(dboObjectToHandle,
									dboDestinationHolding, Tc.COLLECTION_EVENT, svr);
							wr.createPetMovement(dboObjectToHandle, dboDestinationHolding.getVal(Tc.PIC).toString(),
									dboDestinationHolding.getObject_id(), dateOfMovement, Tc.FINISHED,
									currentDboUser.getVal(Tc.USER_NAME).toString(), Tc.RELEASE, sourceArchiveNumber,
									destinationArchiveNumber, false, svw, svr);
							dboObjectToHandle.setParent_id(dboDestinationHolding.getObject_id());
							svw.saveObject(dboObjectToHandle, false);
							break;
						case Tc.ADOPTED:
							DateTime dateOfAdoption = new DateTime(actionDate);
							DbDataObject dboPerson = svr.getObjectById(Long.valueOf(actionParam),
									SvReader.getTypeIdByName(Tc.HOLDING_RESPONSIBLE), null);
							dboObjectToHandle.setStatus(subActionName);
							dboObjectToHandle.setVal(Tc.DT_ADOPTION, dateOfAdoption);
							svw.saveObject(dboObjectToHandle, false);
							wr.linkObjects(dboObjectToHandle, dboPerson, Tc.PET_OWNER, null, svr);
							sourceArchiveNumber = wr.generateArchiveNumber(dboObjectToHandle, dboParentObjectToHandle,
									Tc.RELEASE_EVENT, svr);
							wr.createPetMovement(dboObjectToHandle, null, null, dateOfAdoption, Tc.ADOPTED,
									currentDboUser.getVal(Tc.USER_NAME).toString(), Tc.RELEASE, sourceArchiveNumber,
									null, false, svw, svr);
							break;
						default:
							break;
						}
						break;
					case Tc.PET_MOVEMENT:
						if (actionName.equals(Tc.RETRUN_PET)) {
							DbDataObject dboPet = svr.getObjectById(dboObjectToHandle.getParent_id(),
									SvReader.getTypeIdByName(Tc.PET), null);
							if (dboPet != null) {
								if (!dboPet.getStatus().equals(Tc.RELEASED) && !dboPet.getStatus().equals(Tc.ADOPTED)) {
									throw (new SvException("naits.error.petMustBeReleasedToExecuteThisAction",
											svr.getInstanceUser()));
								}
								String destinationArchiveNumber = wr.generateArchiveNumber(dboPet, dboObjectToHandle,
										Tc.RELEASE_EVENT, svr);
								dboObjectToHandle.setStatus(Tc.FINISHED);
								dboObjectToHandle.setVal(Tc.MOVEMENT_TYPE, Tc.COLLECTED);
								dboObjectToHandle.setVal(Tc.DESTINATION_HOLDING_PIC,
										dboObjectToHandle.getVal(Tc.SOURCE_HOLDING_PIC).toString());
								dboObjectToHandle.setVal(Tc.HOLDING_OBJ_ID, dboPet.getParent_id().toString());
								dboObjectToHandle.setVal(Tc.DEST_HOLD_ARCH_NO, destinationArchiveNumber);
								if (dboObjectToHandle.getVal(Tc.SRC_HOLD_ARCH_NO) == null) {
									String sourceArchiveNumber = wr.generateArchiveNumber(dboPet, null,
											Tc.COLLECTION_EVENT, svr);
									dboObjectToHandle.setVal(Tc.SRC_HOLD_ARCH_NO, sourceArchiveNumber);
								}
								dboPet.setStatus(Tc.VALID);
								svw.saveObject(dboObjectToHandle, false);
								svw.saveObject(dboPet, false);
							}
						}
						break;
					case Tc.PASSPORT_REQUEST:
						if (actionName.equals(Tc.UPDATE_STATUS)) {
							switch (subActionName) {
							case Tc.ACCEPT_REQUEST:
								DbDataObject dboVeterinaryStation = svr.getObjectById(
										Long.valueOf(dboObjectToHandle.getVal(Tc.HOLDING_OBJ_ID).toString()),
										SvReader.getTypeIdByName(Tc.HOLDING), null);
								DbDataObject dboPetPassport = wr.createPetPassport(null,
										dboVeterinaryStation.getVal(Tc.PIC).toString(), null, null, null, null,
										dboObjectToHandle.getParent_id().toString(), dboObjectToHandle.getObject_id());
								dboObjectToHandle.setStatus(Tc.ACCEPTED);
								svw.saveObject(dboPetPassport, false);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.DECLINE_REQUEST:
								dboObjectToHandle.setVal(Tc.DECLINE_REASON, actionParam);
								dboObjectToHandle.setStatus(Tc.DECLINED);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.CANCEL_REQUEST:
								if (!dboObjectToHandle.getStatus().equals(Tc.DRAFT)) {
									throw (new SvException("naits.error.onlyDraftRequestsCanBeCanceled",
											svr.getInstanceUser()));
								}
								dboObjectToHandle.setStatus(Tc.CANCELED);
								svw.saveObject(dboObjectToHandle, false);
								break;
							default:
								break;
							}
							break;
						}
						break;
					case Tc.PET_PASSPORT:
						if (actionName.equals(Tc.UPDATE_STATUS)) {
							if (!dboObjectToHandle.getStatus().equals(Tc.VALID)) {
								throw (new SvException("naits.error.onlyValidPassportsCanBeHandledWithChoosenAction",
										svr.getInstanceUser()));
							}
							switch (subActionName) {
							case Tc.LOST:
								dboObjectToHandle.setStatus(Tc.LOST);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.DAMAGED:
								dboObjectToHandle.setStatus(Tc.DAMAGED);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.EXPIRED:
								dboObjectToHandle.setStatus(Tc.EXPIRED);
								svw.saveObject(dboObjectToHandle, false);
								break;
							default:
								break;
							}
							break;
						}
						break;
					case Tc.HEALTH_PASSPORT:
						if (actionName.equals(Tc.UPDATE_STATUS)) {
							if (!dboObjectToHandle.getStatus().equals(Tc.VALID)) {
								throw (new SvException("naits.error.onlyValidPassportsCanBeHandledWithChoosenAction",
										svr.getInstanceUser()));
							}
							switch (subActionName) {
							case Tc.INVALID:
								dboObjectToHandle.setStatus(Tc.INVALID);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.DAMAGED:
								dboObjectToHandle.setStatus(Tc.DAMAGED);
								svw.saveObject(dboObjectToHandle, false);
								break;
							case Tc.LOST:
								dboObjectToHandle.setStatus(Tc.LOST);
								svw.saveObject(dboObjectToHandle, false);
								break;
							default:
								break;
							}
							break;
						}
						break;
					case Tc.HOLDING:
						if (actionName.equals(Tc.DIRECT_MOVEMENT)) {
							DateTime dateOfMovement = new DateTime(actionDate);
							if (dateOfMovement.isAfter(new DateTime())) {
								throw (new SvException("naits.error.activityDateCannotBeInTheFuture",
										svr.getInstanceUser()));
							}
							DbDataObject dboPet = svr.getObjectById(Long.valueOf(actionParam),
									SvReader.getTypeIdByName(Tc.PET), null);
							String petStatus = dboPet.getStatus();
							if (petStatus.equals(Tc.EUTHANIZED) || petStatus.equals(Tc.DIED)) {
								throw (new SvException("naits.error.collectionOfDeadAnimalsIsNotAllowed",
										svr.getInstanceUser()));
							}
							DbDataObject dboParentHolding = svr.getObjectById(dboPet.getParent_id(),
									dboObjectToHandle.getObject_type(), null);
							ArrayList<String> arrInStatuses = new ArrayList<>();
							arrInStatuses.add(Tc.RELEASED);
							arrInStatuses.add(Tc.ADOPTED);
							DbDataObject dboPetMovement = rdr.getPetMovement(dboPet, arrInStatuses, svr);
							String destinationArchiveNumber = wr.generateArchiveNumber(dboPet, dboObjectToHandle,
									Tc.COLLECTION_EVENT, svr);
							if (dboPetMovement != null) {
								wr.finishPetMovement(dboPet, dboPetMovement, dboObjectToHandle, svr);
								if (arrInStatuses.contains(petStatus)) {
									if (dboParentHolding != null && dboObjectToHandle != null && dboParentHolding
											.getObject_id().equals(dboObjectToHandle.getObject_id())) {
										dboParentHolding = dboObjectToHandle;
									}
								}
								svw.saveObject(dboPetMovement, false);
							} else {
								if (dboParentHolding != null
										&& dboParentHolding.getObject_id().equals(dboObjectToHandle.getObject_id())
										&& !dboPet.getStatus().equals(Tc.ADOPTED)) {
									throw (new SvException("naits.error.petAlreadyBelongToCurrentAnimalShelter",
											svr.getInstanceUser()));
								}
								String sourceArchiveNumber = wr.generateArchiveNumber(dboPet, null, Tc.RELEASE_EVENT,
										svr);
								wr.createPetMovement(dboPet, dboObjectToHandle.getVal(Tc.PIC).toString(),
										dboObjectToHandle.getObject_id(), dateOfMovement, Tc.FINISHED,
										currentDboUser.getVal(Tc.USER_NAME).toString(), Tc.COLLECTION,
										sourceArchiveNumber, destinationArchiveNumber, false, svw, svr);
							}
							if (petStatus.equals(Tc.ADOPTED) && dboPet.getVal(Tc.IS_STRAY_PET) != null
									&& dboPet.getVal(Tc.IS_STRAY_PET).toString().equals("1")) {
								dboPet.setVal(Tc.ADDITIONAL_STATUS, Tc.ABANDONED);
							}
							dboPet.setStatus(Tc.VALID);
							dboPet.setParent_id(dboObjectToHandle.getObject_id());
							svw.saveObject(dboPet, false);
						}
						break;
					case Tc.POPULATION:
						resultMessage = "naits.success.finalizedPopulation";
						if (actionName.equals(Tc.UPDATE_STATUS) && !dboObjectToHandle.getStatus().equals(Tc.DRAFT)) {
							throw (new SvException("naits.error.invalidStatusOfPopulation", svr.getInstanceUser()));
						}
						if (Tc.FINAL.equals(subActionName)) {
							dboObjectToHandle.setStatus(subActionName);
							svw.saveObject(dboObjectToHandle, false);
						}
						break;
					case Tc.RFID_INPUT:
						resultMessage = "naits.success.confirmRfidImportTool";
						DbDataArray arrRfidInputState = svr.getObjectsByParentId(dboObjectToHandle.getObject_id(),
								SvReader.getTypeIdByName(Tc.RFID_INPUT_STATE), null, 0, 0);
						if (!arrRfidInputState.getItems().isEmpty()) {
							throw new SvException("naits.error.rfidInputAlreadyHavePreprocessedObjects",
									svr.getInstanceUser());
						}
						if (actionName.equals(Tc.ACCEPTED)) {
							wr.generateRfidInputState(dboObjectToHandle, rdr, svw, svr);
						}
						break;
					default:
						break;
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svw.dbCommit();
			svg.dbCommit();
		} catch (SvException sve) {
			resultMessage = sve.getLabelCode();
			log4j.error(sve.getFormattedMessage(), sve);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svg != null) {
				svg.release();
			}
		}
		return resultMessage;
	}

	public String animalFlockMassHandler(JsonObject jsonData, String sessionId)
			throws SvException, InterruptedException {
		String result = Tc.successMassAnimalsAction;
		SvReader svr = null;
		SvWriter svw = null;
		SvWorkflow sww = null;
		Writer wr = null;
		Reader rdr = null;
		ValidationChecks vc = null;
		Long holdingObjectId = 0L;
		int counter = 0;
		// ReentrantLock lock = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			sww = new SvWorkflow(svw);

			// MASS ACTION PARAMETERS
			String tableName = null;
			String actionName = null;
			String subActionName = null;
			String actionParam = null;
			String dateOfMovement = null;
			String dateOfAdmission = null;
			String transporterPersonId = null;
			String movementTransportType = null;
			String transporterLicense = null;
			String estmDateArrival = null;
			String estmDateDeparture = null;
			String disinfectionDate = null;
			String animalMvmReason = null;
			String actionDate = null;
			String reasonParam = null;
			Long totalUnits = null;
			Long maleUnits = null;
			Long femaleUnits = null;
			Long adultsUnits = null;
			// list of animal IDs that already participated in selected campaign
			// and would not be vaccinated once again with the same vaccine
			ArrayList<String> animalIDsOfAlreadyVaccinatedAnimals = new ArrayList<String>();

			wr = new Writer();
			rdr = new Reader();
			vc = new ValidationChecks();
			UserManager um = new UserManager();

			svw.setAutoCommit(false);
			sww.setAutoCommit(false);

			JsonArray jsonArrayData = jsonData.get(Tc.OBJ_ARRAY).getAsJsonArray();
			JsonArray jsonParams = jsonData.get(Tc.OBJ_PARAMS).getAsJsonArray();

			// set parameters
			for (JsonElement jsonElement : jsonParams) {
				JsonObject obj = jsonElement.getAsJsonObject();
				if (obj.has(Tc.MASS_PARAM_TBL_NAME)) {
					tableName = obj.get(Tc.MASS_PARAM_TBL_NAME).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ACTION)) {
					actionName = obj.get(Tc.MASS_PARAM_ACTION).getAsString().toUpperCase();
				}
				if (obj.has(Tc.MASS_PARAM_SUBACTION)) {
					subActionName = obj.get(Tc.MASS_PARAM_SUBACTION).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ADDITIONAL_PARAM)) {
					actionParam = obj.get(Tc.MASS_PARAM_ADDITIONAL_PARAM).getAsString().toUpperCase();
				}
				if (obj.has(Tc.MASS_PARAM_DATE_OF_MOVEMENT)) {
					dateOfMovement = obj.get(Tc.MASS_PARAM_DATE_OF_MOVEMENT).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_DATE_OF_ADMISSION)) {
					dateOfAdmission = obj.get(Tc.MASS_PARAM_DATE_OF_ADMISSION).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_TRANSPORTER_PERSON_ID)) {
					transporterPersonId = obj.get(Tc.MASS_PARAM_TRANSPORTER_PERSON_ID).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_MVM_TRANSPORT_TYPE)) {
					movementTransportType = obj.get(Tc.MASS_PARAM_MVM_TRANSPORT_TYPE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_TRANSPORTER_LICENCE)) {
					transporterLicense = obj.get(Tc.MASS_PARAM_TRANSPORTER_LICENCE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ESTM_DATE_OF_ARRIVAL)) {
					estmDateArrival = obj.get(Tc.MASS_PARAM_ESTM_DATE_OF_ARRIVAL).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ESTM_DATE_OF_DEPARTURE)) {
					estmDateDeparture = obj.get(Tc.MASS_PARAM_ESTM_DATE_OF_DEPARTURE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_DISINFECTION_DATE)) {
					disinfectionDate = obj.get(Tc.MASS_PARAM_DISINFECTION_DATE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_ANIMAL_MVM_REASON)) {
					animalMvmReason = obj.get(Tc.MASS_PARAM_ANIMAL_MVM_REASON).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_TOTAL_UNITS)) {
					totalUnits = obj.get(Tc.MASS_PARAM_TOTAL_UNITS).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_MALE_UNITS)) {
					maleUnits = obj.get(Tc.MASS_PARAM_MALE_UNITS).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_FEMALE_UNITS)) {
					femaleUnits = obj.get(Tc.MASS_PARAM_FEMALE_UNITS).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_ADULT_UNITS)) {
					adultsUnits = obj.get(Tc.MASS_PARAM_ADULT_UNITS).getAsLong();
				}
				if (obj.has(Tc.MASS_PARAM_ACTION_DATE)) {
					actionDate = obj.get(Tc.MASS_PARAM_ACTION_DATE).getAsString();
				}
				if (obj.has(Tc.MASS_PARAM_REASON)) {
					reasonParam = obj.get(Tc.MASS_PARAM_REASON).getAsString();
				}
			}

			DbDataObject dboUser = svr.getInstanceUser();
			DbDataObject animalTypeDesc = SvReader.getDbtByName(Tc.ANIMAL);
			DbDataObject flockTypeDesc = SvReader.getDbtByName(Tc.FLOCK);
			DbDataObject vaccEvenetTypeDesc = SvReader.getDbtByName(Tc.VACCINATION_EVENT);

			DbDataObject movementDocObject = null;
			DbDataObject dboHolding = null;
			DbDataObject dboDestinationHolding = null;
			DbDataObject dboMovementDoc = null;
			DbDataObject dboAnimalOrFlockMovement = null;

			String objectTypeLabel = Tc.EMPTY_STRING;
			String regex = Tc.NUMBER_REGEX;

			ArrayList<String> animalOrFlockIds = new ArrayList<>();

			// check if parent exist
			for (JsonElement jsonElement : jsonArrayData) {
				DbDataObject dboObjectToHandle = null;
				try {
					JsonObject obj = jsonElement.getAsJsonObject();
					if (obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID) != null) {
						dboObjectToHandle = svr.getObjectById(obj.get(Tc.ANIMAL + "." + Tc.OBJECT_ID).getAsLong(),
								animalTypeDesc, null);
					}
					if (obj.get(Tc.ANIMAL_MOVEMENT + "." + Tc.PARENT_ID) != null) {
						dboObjectToHandle = svr.getObjectById(
								obj.get(Tc.ANIMAL_MOVEMENT + "." + Tc.PARENT_ID).getAsLong(), animalTypeDesc, null);
					}
					if (obj.get(Tc.ANIMAL_MOVEMENT + "." + Tc.DESTINATION_HOLDING_ID) != null) {
						dboDestinationHolding = rdr.findAppropriateHoldingByPic(obj
								.get(Tc.ANIMAL_MOVEMENT + "." + Tc.DESTINATION_HOLDING_ID).toString().replace("\"", ""),
								svr);
					}
					if (obj.get(Tc.FLOCK_MOVEMENT + "." + Tc.PARENT_ID) != null) {
						dboObjectToHandle = svr.getObjectById(
								obj.get(Tc.FLOCK_MOVEMENT + "." + Tc.PARENT_ID).getAsLong(), flockTypeDesc, null);
					}
					if (obj.get(Tc.FLOCK_MOVEMENT + "." + Tc.DESTINATION_HOLDING_ID) != null) {
						dboDestinationHolding = rdr.findAppropriateHoldingByPic(obj
								.get(Tc.FLOCK_MOVEMENT + "." + Tc.DESTINATION_HOLDING_ID).toString().replace("\"", ""),
								svr);
					}
					if (dboObjectToHandle == null && obj.get(Tc.FLOCK + "." + Tc.OBJECT_ID) != null) {
						dboObjectToHandle = svr.getObjectById(obj.get(Tc.FLOCK + "." + Tc.OBJECT_ID).getAsLong(),
								flockTypeDesc, new DateTime());
					}
					if (dboObjectToHandle != null && Tc.MOVE.equals(actionName)
							&& Tc.FINISH_MOVEMENT.equalsIgnoreCase(subActionName)) {
						DbDataObject refreshDbo = svr.getObjectById(dboObjectToHandle.getObject_id(),
								dboObjectToHandle.getObject_type(), new DateTime());
						dboObjectToHandle = refreshDbo;
					}
					DbDataObject dboVaccEvent = null;
					if (actionParam != null && actionParam.matches(regex)) {
						dboVaccEvent = svr.getObjectById(Long.valueOf(actionParam), vaccEvenetTypeDesc, null);
					}
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.no_animal_found", svr.getInstanceUser()));
					}
					if (dboObjectToHandle.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))) {
						objectTypeLabel = "animal";
					} else if (dboObjectToHandle.getObject_type().equals(SvReader.getTypeIdByName(Tc.FLOCK))) {
						objectTypeLabel = "flock";
						if (Tc.START_MOVEMENT.equalsIgnoreCase(subActionName)) {
							dboObjectToHandle = wr.createFlockMovementUnit(dboObjectToHandle, totalUnits, femaleUnits,
									maleUnits, adultsUnits, svw, svr);
						}
					}
					/*
					 * lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()),
					 * false, 0); if (lock == null) { log4j.info(dboObjectToHandle.getObject_id());
					 * throw (new SvException(Tc.objectUsedByOtherSession, svr.getInstanceUser()));
					 * }
					 */
					DbDataObject dboObjToHandleParent = svr.getObjectById(dboObjectToHandle.getParent_id(),
							SvReader.getTypeIdByName(Tc.HOLDING), null);

					if (dboObjToHandleParent != null
							&& (tableName.equalsIgnoreCase(Tc.ANIMAL) || tableName.equalsIgnoreCase(Tc.FLOCK))
							&& !vc.checkIfHoldingIsSlaughterhouse(dboObjToHandleParent)
							&& !dboObjectToHandle.getStatus().equals(Tc.VALID)
							&& !actionName.equalsIgnoreCase(Tc.UNDO_RETIRE) && !actionName.equalsIgnoreCase(Tc.MOVE)) {
						throw (new SvException("naits.error.onlyValidAnimalCanBeHandledWithChoosenAction",
								svr.getInstanceUser()));
					}
					if (holdingObjectId.equals(0L)) {
						holdingObjectId = dboObjToHandleParent.getObject_id();
					}

					if (Tc.MOVE.equalsIgnoreCase(actionName) && Tc.START_MOVEMENT.equalsIgnoreCase(subActionName)
							&& !holdingObjectId.equals(0L)) {
						counter++;
						if (counter == 1) {
							movementDocObject = wr.createMovementDocument(holdingObjectId, svr);
							movementDocObject.setVal(Tc.MOVEMENT_TYPE, objectTypeLabel.toUpperCase());
							if (movementDocObject != null) {
								svw.saveObject(movementDocObject, false);
							}
						}
					}
					switch (actionName.toUpperCase()) {
					case Tc.RETIRE:
						if (!(Tc.SLAUGHTRD.equalsIgnoreCase(subActionName)
								|| Tc.DESTROYED.equalsIgnoreCase(subActionName))
								&& vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)) {
							throw (new SvException("naits.error.thisStatusIsNotAllowedForSlaughterHouse",
									svr.getInstanceUser()));
						}
						switch (subActionName.toUpperCase()) {
						case Tc.LOST:
							wr.createAnimalOrFlockMovementWithoutDestination(dboObjectToHandle, Tc.LOST, actionDate,
									svr, svw, sww);
							break;
						case Tc.SOLD:
							wr.createAnimalOrFlockMovementWithoutDestination(dboObjectToHandle, Tc.SOLD, actionDate,
									svr, svw, sww);
							break;
						// cases: SLAUGHTRD, DIED, ABSENT DESTROYED
						default:
							boolean isSlaughterhouseOperator = false;
							DbDataArray arrAdditionalGroup = svr.getAllUserGroups(dboUser, false);
							if (!arrAdditionalGroup.getItems().isEmpty()) {
								for (DbDataObject dboUserGroup : arrAdditionalGroup.getItems()) {
									if (dboUserGroup.getVal(Tc.GROUP_NAME) != null && dboUserGroup.getVal(Tc.GROUP_NAME)
											.toString().equals(Tc.SLAUGHTERHOUSE_OPERATOR)) {
										isSlaughterhouseOperator = true;
										break;
									}
								}
							}
							if (Tc.SLAUGHTRD.equalsIgnoreCase(subActionName)) {
								if (Tc.FLOCK.equalsIgnoreCase(objectTypeLabel)
										&& vc.isFlockSlaughterable(dboObjectToHandle)) {
									throw (new SvException("naits.error.cannotSlaughterFlockOfTypeBeehives",
											svCONST.systemUser, null, null));
								}
								if (!dboObjectToHandle.getStatus().equals(Tc.VALID)
										&& !vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)) {
									throw (new SvException("naits.error.onlyValidAnimalsCanBeHandledWithChoosenAction",
											svr.getInstanceUser()));
								}
								vc.slaughterhouseHoldingValidationSet(dboObjectToHandle.getParent_id(),
										dboObjectToHandle, objectTypeLabel, isSlaughterhouseOperator, svr);
								if (vc.checkIfAnimalHasAnyActiveVaccEventForSpecificDisease(dboObjectToHandle,
										actionDate, rdr, svr)) {
									throw (new SvException("naits.error.animalBelongsToCampaignAgainstSpecificDisease",
											svr.getInstanceUser()));
								}
								if (vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)
										&& vc.checkIfAnimalHasAnyActiveVaccEventForSpecificDisease(dboObjectToHandle,
												actionDate, rdr, svr)) {
									throw (new SvException(
											"naits.warning.animalOrFlockHasActiveWithdrawalPeriodAndCannotBeSlaughtered",
											svr.getInstanceUser()));
								}
							}
							if (Tc.DESTROYED.equalsIgnoreCase(subActionName)) {
								if (!vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)) {
									throw (new SvException("naits.error.itemCanNotBeDestroyed", svr.getInstanceUser()));
								}
								if (!(dboObjectToHandle.getStatus().equals(Tc.VALID)
										|| dboObjectToHandle.getStatus().equals(Tc.PREMORTEM))) {
									throw (new SvException("naits.error.onlyValidAnimalsCanBeHandledWithChoosenAction",
											svr.getInstanceUser()));
								}
								if (isSlaughterhouseOperator) {
									throw (new SvException("naits.error.slaughterhouseOperatorNotAllowedToDestroy",
											svr.getInstanceUser()));
								}
								if (vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)
										&& vc.checkIfAnimalHasAnyActiveVaccEventForSpecificDisease(dboObjectToHandle,
												actionDate, rdr, svr)) {
									throw (new SvException(
											"naits.warning.animalOrFlockHasActiveWithdrawalPeriodAndCannotBeDestroyed",
											svr.getInstanceUser()));
								}
							}
							dboObjectToHandle.setStatus(subActionName.toUpperCase());
							if (!dboObjectToHandle.getObject_id().equals(0L)
									&& dboObjectToHandle.getObject_type().equals(SvReader.getTypeIdByName(Tc.ANIMAL))
									&& (Tc.DIED.equalsIgnoreCase(subActionName)
											|| Tc.SLAUGHTRD.equalsIgnoreCase(subActionName)
											|| Tc.DESTROYED.equalsIgnoreCase(subActionName))) {
								if (actionDate != null) {
									DateTime dtDeathDate = new DateTime(actionDate);
									if (dtDeathDate.isAfter(new DateTime())) {
										throw (new SvException("naits.error.deathDateCannotBeInTheFuture",
												svr.getInstanceUser()));
									}
									dboObjectToHandle.setVal(Tc.DEATH_DATE, dtDeathDate);
									if (reasonParam != null) {
										wr.setDestructionNotePerAnimal(dboObjectToHandle, reasonParam, rdr, true, svw,
												svr);
									}
								} else {
									wr.setAutoDate(dboObjectToHandle, Tc.DEATH_DATE, true);
								}
							}
							if (dboObjectToHandle.getIs_dirty()) {
								svw.saveObject(dboObjectToHandle, false);
							}
							break;
						}
						if (!dboObjectToHandle.getStatus().toString().equals(Tc.VALID)) {
							HerdActions hrdAct = new HerdActions();
							if (hrdAct.checkIfAnimalAlreadyBelongsInHerd(dboObjectToHandle.getObject_id(), svr)) {
								hrdAct.removeAnimalFromHerd(dboObjectToHandle.getObject_id(), svr);
							}
						}
						if (dboObjToHandleParent.getStatus().equals(Tc.VALID)
								&& vc.checkIfHoldingIsCommercialOrSubsistenceFarmType(dboObjToHandleParent)) {
							DbDataArray arrAnimals = rdr.getValidAnimalsOrFlockByParentId(
									dboObjToHandleParent.getObject_id(), SvReader.getTypeIdByName(Tc.ANIMAL), svr);
							DbDataArray arrFlocks = rdr.getValidAnimalsOrFlockByParentId(
									dboObjToHandleParent.getObject_id(), SvReader.getTypeIdByName(Tc.FLOCK), svr);
							if ((arrAnimals.getItems().isEmpty() && arrFlocks.getItems().isEmpty())) {
								wr.updateHoldingStatus(dboObjToHandleParent, Tc.VALID, Tc.SUSPENDED, svr);
							}
						}
						// wr.updateHoldingStatus(dboObjToHandleParent,
						// Tc.VALID, Tc.SUSPENDED, true, svr);
						break;
					case Tc.UNDO_RETIRE:
						Boolean disableDoublFinishMovementCase = false;
						switch (dboObjectToHandle.getStatus()) {
						case Tc.TRANSITION:
							throw (new SvException("naits.error.cantUndoRetireOnAnimalWithStatusTransition",
									svr.getInstanceUser()));
						case Tc.PREMORTEM:
							throw (new SvException("naits.error.cantUndoRetireOnAnimalWithStatusPremortem",
									svr.getInstanceUser()));
						case Tc.POSTMORTEM:
							throw (new SvException("naits.error.cantUndoRetireOnAnimalWithStatusPostmortem",
									svr.getInstanceUser()));
						case Tc.SLAUGHTRD:
							if (vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)) {
								throw (new SvException("naits.error.cantUndoRetireOnAnimalWithStatusSlaughter",
										svr.getInstanceUser()));
							}
							break;
						case Tc.EXPORTED:
							if (!um.checkIfUserHasCustomPermission(dboUser, Tc.CUSTOM_UNDO_RETIRE_EXPORT_ANIM, svr))
								throw (new SvException("naits.error." + objectTypeLabel + "CantBeUndoBecaseIsExported",
										svCONST.systemUser, null, null));
							break;
						case Tc.DESTROYED:
							DbDataArray arrPremortem = svr.getObjectsByParentId(dboObjectToHandle.getObject_id(),
									SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), new DateTime(), 0, 0);
							if (!arrPremortem.getItems().isEmpty()) {
								for (DbDataObject dboPreMortem : arrPremortem.getItems()) {
									svw.deleteObject(dboPreMortem, false);
								}
							}
							break;
						case Tc.LOST:
						case Tc.SOLD:
							DbDataArray arrMovements = svr.getObjectsByParentId(dboObjectToHandle.getObject_id(),
									SvReader.getTypeIdByName(Tc.ANIMAL_MOVEMENT), null, 0, 0);
							if (arrMovements != null && !arrMovements.getItems().isEmpty()) {
								for (DbDataObject animalMovement : arrMovements.getItems()) {
									if (animalMovement != null && animalMovement.getStatus().equals(Tc.VALID)
											&& animalMovement.getVal(Tc.MOVEMENT_REASON)
													.equals(dboObjectToHandle.getStatus())
											&& animalMovement.getVal(Tc.DESTINATION_HOLDING_ID) == null) {
										wr.finishAnimalOrFlockMovement(dboObjectToHandle, dboObjToHandleParent, null,
												null, null, svw, sww);
										disableDoublFinishMovementCase = true;
										break;
									}
								}
							}
							break;
						default:
							break;
						}
						if (!disableDoublFinishMovementCase) {
							wr.finishAnimalOrFlockMovement(dboObjectToHandle, dboObjToHandleParent, null, null, null,
									svw, sww);
						}
						wr.undoAnimalPendingExport(dboObjectToHandle, rdr, svr, svw);
						wr.setDateFieldToNull(dboObjectToHandle, Tc.DEATH_DATE, svw);
						// force refresh on animal
						@SuppressWarnings(Tc.UNUSED)
						DbDataObject dboAnimal = svr.getObjectById(dboObjectToHandle.getObject_id(),
								dboObjectToHandle.getObject_type(), new DateTime());
						result = "naits.success.massAnimalsActionUndoRetirement";
						if (dboObjToHandleParent.getStatus().equals(Tc.SUSPENDED)
								&& vc.checkIfHoldingIsCommercialOrSubsistenceFarmType(dboObjToHandleParent)) {
							DbDataArray arrAnimals = rdr.getValidAnimalsOrFlockByParentId(
									dboObjToHandleParent.getObject_id(), SvReader.getTypeIdByName(Tc.ANIMAL), svr);
							DbDataArray arrFlocks = rdr.getValidAnimalsOrFlockByParentId(
									dboObjToHandleParent.getObject_id(), SvReader.getTypeIdByName(Tc.FLOCK), svr);
							if ((!arrAnimals.getItems().isEmpty() || !arrFlocks.getItems().isEmpty())) {
								wr.updateHoldingStatus(dboObjToHandleParent, Tc.SUSPENDED, Tc.VALID, svr);
							}
						}
						break;
					case Tc.ACTIVITY:
						DateTime dtAction = new DateTime(actionDate);
						if (dtAction.isAfter(new DateTime())) {
							throw (new SvException("naits.error.actionDateCannotBeInTheFuture", svr.getInstanceUser()));
						}
						if (!Tc.PHYSICAL_CHECK.equalsIgnoreCase(subActionName) && dboVaccEvent != null) {
							List<String> animalTypes = rdr.getMultiSelectFieldValueAsList(dboVaccEvent, Tc.ANIMAL_TYPE);
							if (!vc.checkIfAnimalTypeScopeIsApplicableOnSelectedAnimal(animalTypes,
									dboObjectToHandle)) {
								throw (new SvException(
										"naits.error." + objectTypeLabel + "CampaignNotApplicableOnSelectedObjects",
										svr.getInstanceUser()));
							}
							animalIDsOfAlreadyVaccinatedAnimals = wr.createVaccTreatmentRecord(dboObjectToHandle,
									dboVaccEvent, actionDate, totalUnits, animalIDsOfAlreadyVaccinatedAnimals, svw,
									svr);
							if (animalIDsOfAlreadyVaccinatedAnimals.size() > 0) {
								result = "naits.info.animalIdsThatAlreadyParticipatedInCampaign" + ": "
										+ animalIDsOfAlreadyVaccinatedAnimals.toString();
							}
						} else {
							Calendar calendar = Calendar.getInstance();
							java.sql.Date dtNow = new java.sql.Date(calendar.getTime().getTime());
							String dateOfAction = dtNow.toString();
							if (actionDate != null) {
								dateOfAction = actionDate;
							}
							DbDataObject vaccBookPhysicalCheck = wr.createVaccinationBook(dateOfAction, "", "", svw);
							vaccBookPhysicalCheck.setVal(Tc.NOTE, "Physical exam");
							if (objectTypeLabel.equalsIgnoreCase(Tc.FLOCK)) {
								if (totalUnits != null && !totalUnits.equals(0L)
										&& dboObjectToHandle.getVal(Tc.TOTAL) != null) {
									if (totalUnits > Long.valueOf(dboObjectToHandle.getVal(Tc.TOTAL).toString())) {
										throw (new SvException("naits.error.numberOfUnitsCantBeLargerThanTotalUnits",
												svr.getInstanceUser()));
									}
									vaccBookPhysicalCheck.setVal(Tc.NO_ITEMS_TREATED, totalUnits);
								} else {
									if (dboObjectToHandle.getVal(Tc.TOTAL) != null) {
										vaccBookPhysicalCheck.setVal(Tc.NO_ITEMS_TREATED,
												Long.valueOf(dboObjectToHandle.getVal(Tc.TOTAL).toString()));
									}
								}
							} else {
								vaccBookPhysicalCheck.setVal(Tc.NO_ITEMS_TREATED, 1L);
							}
							svw.saveObject(vaccBookPhysicalCheck, false);
							wr.linkObjects(dboObjectToHandle, vaccBookPhysicalCheck,
									objectTypeLabel.toUpperCase() + "_VACC_BOOK",
									"link between " + objectTypeLabel.toUpperCase() + " and VACC_BOOK by physical exam",
									svr);
						}
						break;
					case Tc.MOVE:
						if (actionParam == null) {
							throw (new SvException("naits.error.destination_holding_missing", svr.getInstanceUser()));
						}
						dboHolding = svr.getObjectById(Long.valueOf(actionParam), SvReader.getTypeIdByName(Tc.HOLDING),
								null);
						if (Tc.CANCEL_MOVEMENT.equalsIgnoreCase(subActionName)
								&& (dboObjectToHandle.getStatus().equals(Tc.LOST)
										|| dboObjectToHandle.getStatus().equals(Tc.SOLD))) {
							throw (new SvException(
									"naits.error.actionCancelMovementCannotBeExecutedOnAnimalsWithStatusSoldOrLost",
									svr.getInstanceUser()));
						}
						if (!(dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.VALID)
								|| dboObjectToHandle.getStatus().equalsIgnoreCase(Tc.TRANSITION))
								&& !(Tc.FINISH_MOVEMENT.equalsIgnoreCase(subActionName)
										|| Tc.START_MOVEMENT.equalsIgnoreCase(subActionName)
										|| Tc.CANCEL_MOVEMENT.equalsIgnoreCase(subActionName)
										|| Tc.FINISH_MOVEMENT_SLAUGHTR.equalsIgnoreCase(subActionName))) {
							throw (new SvException("naits.error." + objectTypeLabel + "IsNotValidForMovement",
									svr.getInstanceUser()));
						}
						// Movement doc of current movement
						if (dboObjectToHandle != null && !Tc.START_MOVEMENT.equals(subActionName)
								&& dboDestinationHolding != null && dboDestinationHolding.getVal(Tc.PIC) != null) {
							String destinationHoldingPic = dboDestinationHolding.getVal(Tc.PIC).toString();
							DbDataArray dboAnimalOrFlockMovementsArr = rdr.getExistingAnimalOrFlockMovements(
									dboObjectToHandle, destinationHoldingPic, Tc.VALID, svr);
							if (dboAnimalOrFlockMovementsArr != null
									&& !dboAnimalOrFlockMovementsArr.getItems().isEmpty()) {
								dboAnimalOrFlockMovement = dboAnimalOrFlockMovementsArr.get(0);
								if (dboAnimalOrFlockMovement.getVal(Tc.MOVEMENT_DOC_ID) != null) {
									dboMovementDoc = rdr.searchForObject(SvReader.getTypeIdByName(Tc.MOVEMENT_DOC),
											Tc.MOVEMENT_DOC_ID,
											dboAnimalOrFlockMovement.getVal(Tc.MOVEMENT_DOC_ID).toString(), svr);
								}
							}
						}
						// IMPORTANT: when there is active export quarantine,
						// the
						// source
						// holding can move animals out, issue #177, block is
						// present only for disease/blacklist quarantine
						if (dboHolding != null && dboObjectToHandle.getParent_id().equals(dboHolding.getObject_id())
								&& !Tc.CANCEL_MOVEMENT.equalsIgnoreCase(subActionName)) {
							throw (new SvException("naits.error.sourceAndDestinationHoldingAreSame",
									svr.getInstanceUser()));
						}
						if (dboObjectToHandle != null) {
							if (Tc.START_MOVEMENT.equals(subActionName)) {
								if (!rdr.checkIfAnimalOrFlockMovementExist(dboObjectToHandle, dboHolding, svr)
										&& vc.checkIfAnimalOrFlockDoesNotBelongToDraftMovementDoc(dboObjectToHandle,
												rdr, svr)) {
									if (movementDocObject.getVal(Tc.DESTINATION_HOLDING_PIC) == null) {
										movementDocObject.setVal(Tc.DESTINATION_HOLDING_PIC,
												dboHolding.getVal(Tc.PIC) != null ? dboHolding.getVal(Tc.PIC).toString()
														: null);
										svw.saveObject(movementDocObject, false);
									}
									wr.startAnimalOrFlockMovement(dboObjectToHandle, dboHolding, Tc.TRANSFER,
											dateOfMovement, movementDocObject.getVal(Tc.MOVEMENT_DOC_ID).toString(),
											movementTransportType, transporterLicense, estmDateArrival,
											estmDateDeparture, disinfectionDate, animalMvmReason, svr, svw, sww);
								} else {
									throw (new SvException("naits.error.movementForSelectedAnimalAlreadyExists",
											svr.getInstanceUser()));
								}
							} else if (Tc.FINISH_MOVEMENT.equals(subActionName)) {
								wr.finishAnimalOrFlockMovement(dboObjectToHandle, dboHolding, dateOfMovement, null,
										null, svw, sww);
							} else if (Tc.FINISH_MOVEMENT_SLAUGHTR.equals(subActionName)) {
								wr.finishAnimalOrFlockMovement(dboObjectToHandle, dboHolding, dateOfMovement,
										dateOfAdmission, transporterPersonId, svw, sww);
							} else if (Tc.CANCEL_MOVEMENT.equals(subActionName)) {
								if (dboMovementDoc != null && dboMovementDoc.getStatus().equals(Tc.INVALID)) {
									throw (new SvException("naits.error." + objectTypeLabel + "invalidStatus",
											svr.getInstanceUser()));
								}
								wr.cancelMovement(dboObjectToHandle, sww, svr);
								if (dboMovementDoc != null) {
									sww.moveObject(dboMovementDoc, Tc.CANCELED, false);
								}
							}
						}
						if (dboObjectToHandle != null && dboDestinationHolding != null
								&& dboDestinationHolding.getVal(Tc.PIC) != null
								&& (Tc.CANCEL_MOVEMENT.equals(subActionName) || Tc.FINISH_MOVEMENT.equals(subActionName)
										|| Tc.FINISH_MOVEMENT_SLAUGHTR.equals(subActionName))) {
							if (dboMovementDoc != null
									&& vc.calcMovDocumentStatusAccordingMovements(dboMovementDoc, rdr, svr)
											.equals(Tc.RELEASED)) {
								if (dboMovementDoc.getStatus().equals(Tc.INVALID)) {
									throw (new SvException("naits.error.mvmDocHaveMvmDocBlocked" + objectTypeLabel,
											svr.getInstanceUser()));
								}
								// TODO this is quick fix for #1325, should be
								// additionally reviewed and properly modified.
								// @ZPE
								if (!subActionName.equals(Tc.CANCEL_MOVEMENT))
									sww.moveObject(dboMovementDoc, Tc.RELEASED, false);
							} else if (dboMovementDoc != null
									&& vc.calcMovDocumentStatusAccordingMovements(dboMovementDoc, rdr, svr)
											.equals(Tc.CANCELED)) {
								sww.moveObject(dboMovementDoc, Tc.CANCELED, false);
							}
						}
						break;
					// case reverse/undo last animal transfer - for ANIMALS only (not flocks)
					case Tc.UNDO_LAST_TRANSFER:
						DbDataObject lastAnimalMovement = rdr.findLastAnimalMovement(dboObjectToHandle.getObject_id(),
								dboObjToHandleParent, svr);
						if (lastAnimalMovement != null) {
							String lastMovementOriginPic = lastAnimalMovement.getVal(Tc.SOURCE_HOLDING_ID).toString();
							DbDataObject lastMovementOriginHoldingObj = rdr
									.findAppropriateHoldingByPic(lastMovementOriginPic, svr);
							JsonObject jObj = new JsonObject();
							JsonArray subJsonParams = new JsonArray();
							JsonObject jObj1 = new JsonObject();
							jObj1.addProperty(Tc.MASS_PARAM_ANIMAL_FLOCK_ID,
									dboObjectToHandle.getVal(Tc.ANIMAL_ID).toString());
							subJsonParams.add(jObj1);
							JsonObject jObj2 = new JsonObject();
							jObj2.addProperty(Tc.MASS_PARAM_HOLDING_OBJ_ID,
									lastMovementOriginHoldingObj.getObject_id());
							subJsonParams.add(jObj2);
							JsonObject jObj3 = new JsonObject();
							jObj3.addProperty(Tc.MASS_PARAM_ANIMAL_CLASS,
									dboObjectToHandle.getVal(Tc.ANIMAL_CLASS).toString());
							subJsonParams.add(jObj3);
							jObj.add(Tc.OBJ_PARAMS, subJsonParams);
							wr.moveAnimalOrFlockViaDirectTransfer(jObj, sessionId);
						}
						break;
					case Tc.OTHER:
						switch (subActionName.toUpperCase()) {
						case Tc.GENERATE_PREMORTEM:
							DbDataArray arrPreMortemForm = svr.getObjectsByParentId(dboObjectToHandle.getObject_id(),
									SvReader.getTypeIdByName(Tc.PRE_SLAUGHT_FORM), null, 0, 0);
							if (!vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)) {
								throw (new SvException("naits.error.selectedAnimalsDoesNotBelongToSlaughterhouse",
										svr.getInstanceUser()));
							}
							if (!dboObjectToHandle.getStatus().equals(Tc.VALID)
									&& arrPreMortemForm.getItems().isEmpty()) {
								throw (new SvException("naits.error.selectedAnimalIsNotAllowedForPreMortemAction",
										svr.getInstanceUser()));
							}
							if (dboObjectToHandle != null
									&& vc.checkIfAnimalOrFlockHasAlreadyValidPreMortem(dboObjectToHandle, svr)) {
								throw (new SvException("naits.error.animalOrFlockAlreadyHasValidPreMortemForm",
										svCONST.systemUser, null, null));
							}
							if (vc.checkIfAnimalHasBlockingDiseaseInPremortemForm(dboObjectToHandle, svr)) {
								throw (new SvException("naits.error.blockingDiseaseInPreMortem", svCONST.systemUser,
										null, null));
							}
							if (vc.checkIfHoldingIsSlaughterhouse(dboObjectToHandle.getParent_id(), svr)
									&& vc.checkIfAnimalHasAnyActiveVaccEventForSpecificDisease(dboObjectToHandle,
											actionDate, rdr, svr)) {
								throw (new SvException(
										"naits.warning.animalOrFlockHasActiveWithdrawalPeriodAndPremortemFormCannotBeGenerated",
										svr.getInstanceUser()));
							}
							if (dboObjectToHandle != null && (dboObjectToHandle.getStatus().equals(Tc.VALID)
									|| dboObjectToHandle.getStatus().equals(Tc.PREMORTEM))) {
								DbDataObject preMortem = wr.createPreMortemForm("CLINIC_EXAM", "1",
										dboObjectToHandle.getObject_id());
								preMortem.setVal(Tc.IS_AUTO_GEN, Boolean.TRUE.toString());
								svw.saveObject(preMortem, false);
							} else {
								result = "naits.error.animalOrFlockWithPermissableToKillPreMortemForm";
							}
							break;
						case Tc.GENERATE_POSTMORTEM:
							if (!(dboObjectToHandle.getStatus().equals(Tc.SLAUGHTRD)
									|| dboObjectToHandle.getStatus().equals(Tc.POSTMORTEM))) {
								throw (new SvException(
										"naits.error.onlyValidSlaughteredAnimalCanBeHandledWithChoosenAction",
										svr.getInstanceUser()));
							}
							if (dboObjectToHandle != null) {
								DbDataObject postMortemObj = wr.createPostMortem(null, "2", "1", "1",
										dboObjectToHandle.getObject_id());
								postMortemObj.setVal(Tc.IS_AUTO_GEN, Boolean.TRUE.toString());
								svw.saveObject(postMortemObj, false);
							}
							break;
						default:
							break;
						}
						break;
					default:
						break;
					}
					if (dboObjectToHandle != null) {
						@SuppressWarnings(Tc.UNUSED)
						DbDataObject refreshMovementCache = svr.getObjectById(dboObjectToHandle.getObject_id(),
								dboObjectToHandle.getObject_type(), new DateTime());
					}
					if (dboObjToHandleParent != null) {
						@SuppressWarnings(Tc.UNUSED)
						DbDataObject refreshMovementCache2 = svr.getObjectById(dboObjToHandleParent.getObject_id(),
								dboObjToHandleParent.getObject_type(), new DateTime());
					}
				} finally {
					/*
					 * if (lock != null && dboObjectToHandle != null) {
					 * SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock); }
					 */
				}
			}
			if (animalOrFlockIds != null && !animalOrFlockIds.isEmpty()) {
				result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
						"naits.info.unsuccessfullyFinishedAnimalMovements") + " " + animalOrFlockIds.toString();
			}
			svw.dbCommit();
			sww.dbCommit();
			if (actionName.equalsIgnoreCase(Tc.MOVE) && subActionName.equalsIgnoreCase(Tc.START_MOVEMENT)
					&& movementDocObject != null) {
				result = checkAnimalOrFlockMovementsInMovementDocument(movementDocObject, svr.getSessionId());
				if (!result.equals(Tc.successCheckMovementsInMvmDoc)) {
					movementDocObject.setStatus(Tc.INVALID);
				} else {
					result = result + "_" + movementDocObject.getObject_id().toString();
				}
				svw.saveObject(movementDocObject);
				svw.dbCommit();
			}
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
			result = sve.getLabelCode();
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
		return result;
	}
}