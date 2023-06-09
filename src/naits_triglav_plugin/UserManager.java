package naits_triglav_plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvCore;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLink;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvUtil;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

public class UserManager {

	static final Logger log4j = LogManager.getLogger(UserManager.class.getName());

	public ArrayList<String> loadPermissionPackages() {
		ArrayList<String> permissionPackages = new ArrayList<>();
		permissionPackages.add("PERSON_REGISTRATOR_PCK:HOLDING_RESPONSIBLE.FULL");
		permissionPackages.add("HOLDING_REGISTRATOR_PCK:HOLDING.FULL");
		permissionPackages.add(
				"ANIMAL_REGISTRATOR_PCK:HOLDING.READ, HOLDING_RESPONSIBLE.READ, ANIMAL.FULL, ANIMAL_MOVEMENT.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.READ, EAR_TAG_REPLC.FULL, INVENTORY_ITEM.FULL");
		permissionPackages.add(
				"FLOCK_REGISTRATOR_PCK:HOLDING.READ, HOLDING_RESPONSIBLE.READ, ANIMAL.READ, ANIMAL_MOVEMENT.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.FULL, ANIMAL_TYPE.READ, DISEASE.READ");
		permissionPackages.add(
				"ANIMAL_MOVEMENT_REGISTRATOR_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.FULL, ANIMAL.FULL, EAR_TAG_REPLC.READ, MOVEMENT_DOC.FULL, ANIMAL_MOVEMENT.FULL, VACCINATION_EVENT.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, MOVEMENT_DOC_BLOCK.FULL, LAB_SAMPLE.READ, INVENTORY_ITEM.READ, ANIMAL_TYPE.READ, DISEASE.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.READ, AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add(
				"FLOCK_MOVEMENT_REGISTRATOR:HOLDING.FULL, HOLDING_RESPONSIBLE.FULL,FLOCK_MOVEMENT.FULL, QUARANTINE.READ, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, MOVEMENT_DOC_BLOCK.FULL,LAB_SAMPLE.READ, ANIMAL.READ, ANIMAL_MOVEMENT.READ, PET.READ, AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add(
				"FULL_HOLDING_ADMINISTRATOR_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.FULL, ANIMAL.FULL, FLOCK.FULL, EAR_TAG_REPLC.FULL, MOVEMENT_DOC.FULL, FLOCK_MOVEMENT.FULL, ANIMAL_MOVEMENT.FULL, QUARANTINE.READ, VACCINATION_EVENT.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, MOVEMENT_DOC_BLOCK.FULL, SPOT_CHECK.FULL, LAB_SAMPLE.READ, INVENTORY_ITEM.FULL, ANIMAL_TYPE.READ, DISEASE.READ, PET.FULL, AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add(
				"QUARANTINE_WATCHER_PCK:QUARANTINE.READ, EXPORT_CERT.READ, ANIMAL_ORIGIN.READ, ANIMAL_ACTIVITY.READ, HOLDING.READ, HOLDING_RESPONSIBLE.READ, ANIMAL.READ, FLOCK.READ, MOVEMENT_DOC.READ, FLOCK_MOVEMENT.READ, ANIMAL_MOVEMENT.READ, MOVEMENT_DOC_BLOCK.READ, ANIMAL_TYPE.READ, DISEASE.READ, PET.READ");
		permissionPackages.add(
				"SLAUGHTERHOUSE_ADMINISTRATOR_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.FULL, ANIMAL.FULL, FLOCK.FULL, EAR_TAG_REPLC.FULL, MOVEMENT_DOC.FULL, FLOCK_MOVEMENT.FULL, ANIMAL_MOVEMENT.FULL, QUARANTINE.READ, VACCINATION_EVENT.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, MOVEMENT_DOC_BLOCK.FULL, SPOT_CHECK.FULL, LAB_SAMPLE.READ, AREA.FULL, AREA_HEALTH.FULL, PRE_SLAUGHT_FORM.FULL, POST_SLAUGHT_FORM.FULL, INVENTORY_ITEM.FULL, ANIMAL_TYPE.READ, DISEASE.READ");
		permissionPackages.add(
				"QUARANTINE_ADMINISTRATOR_PCK:QUARANTINE.FULL, EXPORT_CERT.FULL, ANIMAL_ORIGIN.FULL, ANIMAL_ACTIVITY.FULL, HOLDING.READ, HOLDING_RESPONSIBLE.READ, ANIMAL.READ, FLOCK.READ, MOVEMENT_DOC.READ, FLOCK_MOVEMENT.READ, ANIMAL_MOVEMENT.READ, MOVEMENT_DOC_BLOCK.READ, INVENTORY_ITEM.READ, ANIMAL_TYPE.READ, DISEASE.READ, PET.READ");
		permissionPackages.add(
				"INVENTORY_WATCHER_PCK:INVENTORY_ITEM.READ, TRANSFER.READ, ORDER.READ, SUPPLIER.READ, RANGE.READ, ANIMAL.READ, ANIMAL_TYPE.READ, DISEASE.READ");
		permissionPackages.add(
				"INVENTORY_ADMINISTRATOR_PCK:INVENTORY_ITEM.FULL, TRANSFER.FULL, ORDER.FULL, SUPPLIER.FULL, RANGE.FULL, ANIMAL.FULL, ANIMAL_TYPE.READ, DISEASE.READ, PET.FULL");
		permissionPackages.add("AHSMT_WATCHER:AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add("AHSMT_ADMINSTRATOR_PCK:AREA.FULL, AREA_HEALTH.FULL");
		permissionPackages.add(
				"RYSK_ANALYZE_ADMINISTRATOR_PCK:HOLDING.READ, ANIMAL.READ, HOLDING_RESPONSIBLE.READ, SELECTION_RESULT.FULL, POPULATION.FULL, SAMPLE.FULL, CRITERIA.FULL, CRITERIA_TYPE.FULL, STRAT_FILTER.FULL, ANIMAL_TYPE.READ, DISEASE.READ, POPULATION_LOCATION.FULL");
		permissionPackages.add(
				"FVIRO_PCK:HOLDING.READ, ANIMAL.READ, HOLDING_RESPONSIBLE.READ, LAB_SAMPLE.FULL, LAB_TEST_RESULT.FULL, LABORATORY.FULL, LAB_TEST_TYPE.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, VACCINATION_EVENT.FULL, ANIMAL_TYPE.READ, DISEASE.READ, ANIMAL_MOVEMENT.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.READ, RFID_INPUT.FULL, RFID_INPUT_STATE.FULL, RFID_INPUT_RESULT.FULL");
		permissionPackages.add(
				"CVIRO_PCK:HOLDING.READ, ANIMAL.READ, HOLDING_RESPONSIBLE.READ, LAB_SAMPLE.FULL, LAB_TEST_RESULT.FULL, LABORATORY.FULL, LAB_TEST_TYPE.FULL, VACCINATION_BOOK.FULL, VACCINATION_EVENT.FULL, VACCINATION_RESULTS.FULL, ANIMAL_TYPE.READ, DISEASE.READ, ANIMAL_MOVEMENT.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.READ");
		permissionPackages.add(
				"LABORANT_PCK:LABORATORY.FULL, LAB_SAMPLE.FULL, LAB_TEST_RESULT.FULL, LAB_TEST_TYPE.FULL, VACCINATION_RESULTS.FULL, ANIMAL.READ, ANIMAL_TYPE.READ, HOLDING.READ, HOLDING_RESPONSIBLE.READ");
		permissionPackages.add("SPOT_CHECK_REGISTRATOR_PCK:SPOT_CHECK.FULL");
		permissionPackages.add(
				"PRIVATE_VETERINARIANS:HOLDING.READ, ANIMAL.READ, HOLDING_RESPONSIBLE.READ,VACCINATION_EVENT.READ, LAB_TEST_RESULT.READ, LAB_TEST_TYPE.READ, LABORATORY.READ, LAB_SAMPLE.FULL,VACCINATION_BOOK.FULL, AREA.READ, PET.FULL, PET_HEALTH_BOOK.FULL, PET_PASSPORT.FULL, PASSPORT_REQUEST.FULL, ANIMAL_TYPE.READ, DISEASE.READ, HEALTH_PASSPORT.FULL, STRAY_PET_LOCATION.FULL, PET_MOVEMENT.FULL, MEASUREMENT.FULL");
		permissionPackages.add(
				"PET_REGISTRATOR_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.READ, VACCINATION_EVENT.READ, VACCINATION_BOOK.FULL, AREA.READ, STRAY_PET.FULL, PET.FULL, PET_HEALTH_BOOK.FULL, PET_PASSPORT.FULL, PASSPORT_REQUEST.FULL, INVENTORY_ITEM.FULL, HEALTH_PASSPORT.FULL, STRAY_PET_LOCATION.FULL, PET_MOVEMENT.FULL, MEASUREMENT.FULL");
		permissionPackages.add(
				"SLAUGHTERHOUSE_OPERATOR_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.READ, FLOCK.FULL, FLOCK_MOVEMENT.FULL, ANIMAL.FULL, ANIMAL_MOVEMENT.FULL, MOVEMENT_DOC.FULL, MOVEMENT_DOC_BLOCK.FULL, INVENTORY_ITEM.FULL, ANIMAL_TYPE.READ, DISEASE.READ, VACCINATION_EVENT.READ, VACCINATION_BOOK.READ, LAB_SAMPLE.READ, LAB_TEST_RESULT.READ, PRE_SLAUGHT_FORM.READ, POST_SLAUGHT_FORM.READ, EAR_TAG_REPLC.FULL, QUARANTINE.READ, AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add("STATISTIC_REPORT_PCK:custom.statistical_report");
		permissionPackages.add("BLANK_REPORT_PCK:custom.blank_report");
		permissionPackages.add("VILLAGE_SPEC_REPORT_PCK:custom.village_specific_report");
		permissionPackages.add("INVOICE_REPORT_PCK:custom.invoice_report");
		permissionPackages.add("GENERAL_REPORT_PCK:custom.general_report");
		permissionPackages.add(
				"RFID_IMPORT_TOOL_SEARCH_PCK:custom.rfid_input_search, RFID_INPUT.FULL, RFID_INPUT_STATE.FULL, RFID_INPUT_RESULT.FULL");
		permissionPackages.add("RFID_MASS_ACTION_PCK:custom.rfid_action");
		permissionPackages.add("RFID_DIRECT_TRANSFER_PCK:custom.rfid_transfer");
		permissionPackages.add("RFID_REGISTER_PCK:custom.rfid_registration");
		permissionPackages.add("RFID_EXPORT_PCK:custom.rfid_export");
		permissionPackages.add("RFID_MOVE_TO_CERT_PCK:custom.rfid_move_to_cert");
		permissionPackages.add(
				"MOVEMENT_DOC_SEARCH_PCK:custom.movement_doc_search, HOLDING.READ, HOLDING_RESPONSIBLE.READ, ANIMAL.READ, EAR_TAG_REPLC.READ, MOVEMENT_DOC.READ, ANIMAL_MOVEMENT.FULL, MOVEMENT_DOC_BLOCK.FULL, INVENTORY_ITEM.READ, ANIMAL_TYPE.READ, PET.READ, FLOCK_MOVEMENT.READ, FLOCK.READ, AREA.READ, AREA_HEALTH.READ");
		permissionPackages.add(
				"ANIMAL_SEARCH_PCK:custom.animal_search, ANIMAL.READ, EAR_TAG_REPLC.READ, INVENTORY_ITEM.READ, ANIMAL_TYPE.READ");
		permissionPackages.add(
				"CAMPAIGN_REGISTRATION_PCK:VACCINATION_EVENT.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL");
		permissionPackages.add(
				"VACCINATION_APPLY_PCK:VACCINATION_EVENT.READ, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, ANIMAL.FULL, FLOCK.FULL, HOLDING.READ, HOLDING_RESPONSIBLE.READ, LABORATORY.READ, LAB_SAMPLE.FULL, LAB_TEST_RESULT.READ, LAB_TEST_TYPE.READ, DISEASE.READ, PET.FULL, PET_HEALTH_BOOK.FULL, PET_PASSPORT.READ, HEALTH_PASSPORT.READ");
		permissionPackages.add(
				"POLICE_PCK:ANIMAL.READ, HOLDING.READ, HOLDING_RESPONSIBLE.READ, EXPORT_CERT.READ, custom.movement_doc_search, MOVEMENT_DOC.READ, MOVEMENT_DOC_BLOCK.READ, ANIMAL_MOVEMENT.READ, FLOCK.READ, FLOCK_MOVEMENT.READ, svarog_read_access");
		permissionPackages.add(
				"ANIMAL_GROUP_PCK:HOLDING.FULL, HOLDING_RESPONSIBLE.FULL, ANIMAL.FULL, ANIMAL_MOVEMENT.FULL, HERD.FULL, HERD_HEALTH_BOOK.FULL, HERD_MOVEMENT.FULL, LAB_SAMPLE.FULL, LAB_TEST_RESULT.FULL, LAB_TEST_TYPE.FULL, EAR_TAG_REPLC.READ, MOVEMENT_DOC.FULL, MOVEMENT_DOC_BLOCK.FULL, VACCINATION_BOOK.FULL, VACCINATION_RESULTS.FULL, AREA.READ, AREA_HEALTH.READ");
		permissionPackages
				.add("FVR_CONFIGURATION_PCK:custom.field_visit_report_configuration, HOLDING.READ, ANIMAL.READ");
		permissionPackages.add(
				"FVR_REGISTRATION_PCK:custom.field_visit_report_registration, HOLDING.READ, ANIMAL.READ, FFT_SCORE.FULL, FF_SCORE.FULL");
		permissionPackages.add("BORDER_POINT_PCK:border_point_management");
		permissionPackages.add("HERD_MODULE_READ_PCK:HERD.READ,HERD_MOVEMENT.READ,HERD_HEALTH_BOOK.READ");
		permissionPackages.add("HERD_MODULE_FULL_PCK:HERD.FULL,HERD_MOVEMENT.FULL,HERD_HEALTH_BOOK.FULL");
		return permissionPackages;
	}

	public String attachUserToGroup(String groupName, JsonArray jsonData, String sessionId) throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		String resultMsgLabel = Tc.EMPTY_STRING;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			rdr = new Reader();
			DbDataObject dboGroup = findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, Tc.GROUP_NAME, groupName, svr);

			if (dboGroup != null)
				// check if parent exist
				for (int i = 0; i < jsonData.size(); i++) {
					Boolean groupSuccesfullyAdded = false;
					JsonObject obj = jsonData.get(i).getAsJsonObject();
					DbDataObject dboUser = null;
					if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
						dboUser = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
								svCONST.OBJECT_TYPE_USER, null);
					}
					if (dboUser == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboUser.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						try {
							DbDataArray allDefaultGroups = svr.getAllUserGroups(dboUser, true);
							DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboUser, false);

							// case 1
							if (allDefaultGroups.getItems().isEmpty()) {
								svs.addUserToGroup(dboUser, dboGroup, true);
								groupSuccesfullyAdded = true;
							}
							// case 2
							if (!groupSuccesfullyAdded && allAdditionalGroups.size() == 0) {
								svs.addUserToGroup(dboUser, dboGroup, false);
								groupSuccesfullyAdded = true;
							}
							// case 3
							Boolean groupFound = false;
							if (!groupSuccesfullyAdded && !allAdditionalGroups.getItems().isEmpty()) {
								for (DbDataObject tempGroup : allAdditionalGroups.getItems()) {
									if ((tempGroup.getVal(Tc.GROUP_TYPE) != null && tempGroup.getVal(Tc.GROUP_TYPE)
											.toString().equals(dboGroup.getVal(Tc.GROUP_TYPE)))
											&& (tempGroup.getVal(Tc.GROUP_NAME) != null
													&& tempGroup.getVal(Tc.GROUP_NAME).toString()
															.equals(dboGroup.getVal(Tc.GROUP_NAME)))) {
										groupFound = true;
										break;
									}
								}
								if (!groupFound) {
									if (dboGroup.getVal(Tc.GROUP_TYPE) != null
											&& dboGroup.getVal(Tc.GROUP_TYPE).toString().equals(Tc.ADMINISTRATORS)) {
										if (checkIfUserLinkedToDefaultGroup(dboUser, Tc.USERS, svr)) {
											DbDataObject dboDefaultGroup = allDefaultGroups.get(0);
											DbDataObject dboDefaultLinkBetweenUserAndUserGroup = SvReader.getLinkType(
													Tc.USER_DEFAULT_GROUP, dboUser.getObject_type(),
													dboDefaultGroup.getObject_type());
											DbDataObject dboLink = rdr.getLinkObject(dboUser.getObject_id(),
													dboDefaultGroup.getObject_id(),
													dboDefaultLinkBetweenUserAndUserGroup.getObject_id(), false, svr);
											if (dboLink != null) {
												svw.deleteObject(dboLink, true);
												svs.addUserToGroup(dboUser, dboGroup, true);
											}
										}
										groupSuccesfullyAdded = true;
									} else {
										svs.addUserToGroup(dboUser, dboGroup, false);
										groupSuccesfullyAdded = true;
									}

								} else {
									resultMsgLabel = "naits.error.admConsole.UserAlreadyBelongsToTheSelectedGroup";
								}
							}
							if (groupSuccesfullyAdded) {
								resultMsgLabel = "naits.success.admConsole.userToGroupAttached";
							}
							ArrayList<String> currUserPermissions = getCurrentUserPermissions(
									dboUser.getVal(Tc.USER_NAME).toString(), svr);
							ArrayList<String> currGroupPermissions = getCurrentGroupPermissions(
									dboGroup.getVal(Tc.GROUP_NAME).toString(), svr);
							if (!currUserPermissions.contains("system.null_geometry") || (groupSuccesfullyAdded
									&& !currGroupPermissions.contains("system.null_geometry"))) {
								// set mandatory to each user assigned to a
								// group
								ArrayList<String> mandatoryPermissions = new ArrayList<String>();
								mandatoryPermissions.add("system.null_geometry");
								setSidPermission(dboUser.getVal(Tc.USER_NAME).toString(), svCONST.OBJECT_TYPE_USER,
										mandatoryPermissions, Tc.GRANT, svr.getSessionId());
							}
						} catch (SvException e) {
							log4j.error(e.getFormattedMessage());
						}
					} finally {
						if (lock != null && dboUser != null) {
							SvLock.releaseLock(String.valueOf(dboUser.getObject_id()), lock);
						}
					}
				}
			svw.dbCommit();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return resultMsgLabel;
	}

	public String detachUserToGroup(String groupName, JsonArray jsonData, String sessionId) throws Exception {
		String resultMsgLabel = "naits.error.userDoesntBelongToTheSelectedGroup";
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);

			DbDataObject dboGroup = findUserOrUserGroup(svCONST.OBJECT_TYPE_GROUP, Tc.GROUP_NAME, groupName, svr);

			if (dboGroup != null)
				// check if parent exist
				for (int i = 0; i < jsonData.size(); i++) {
					JsonObject obj = jsonData.get(i).getAsJsonObject();
					DbDataObject dboObjectToHandle = null;
					if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
						dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
								svCONST.OBJECT_TYPE_USER, null);
					}
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						try {
							DbDataArray allDefaultGroups = svr.getAllUserGroups(dboObjectToHandle, true);
							DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboObjectToHandle, false);
							ArrayList<DbDataObject> convertedAllDefaultGroups = allDefaultGroups.getItems();
							ArrayList<DbDataObject> convertedAllAdditionalGroups = allAdditionalGroups.getItems();
							if (!convertedAllDefaultGroups.isEmpty()) {
								for (DbDataObject tempGroup : convertedAllDefaultGroups) {
									if ((tempGroup.getVal(Tc.GROUP_NAME) != null && tempGroup.getVal(Tc.GROUP_NAME)
											.toString().equals(dboGroup.getVal(Tc.GROUP_NAME)))
											|| (tempGroup.getVal(Tc.GROUP_TYPE) != null
													&& tempGroup.getVal(Tc.GROUP_TYPE).toString()
															.equals(dboGroup.getVal(Tc.GROUP_TYPE)))) {
										svs.removeUserFromGroup(dboObjectToHandle, dboGroup);
										resultMsgLabel = "naits.success.admConsole.userToGroupDetached";
										break;
									}
								}

							}
							if (!convertedAllAdditionalGroups.isEmpty()) {
								for (DbDataObject tempGroup : convertedAllAdditionalGroups) {
									if ((tempGroup.getVal(Tc.GROUP_NAME) != null && tempGroup.getVal(Tc.GROUP_NAME)
											.toString().equals(dboGroup.getVal(Tc.GROUP_NAME)))
											|| (tempGroup.getVal(Tc.GROUP_TYPE) != null
													&& tempGroup.getVal(Tc.GROUP_TYPE).toString()
															.equals(dboGroup.getVal(Tc.GROUP_TYPE)))) {
										svs.removeUserFromGroup(dboObjectToHandle, dboGroup);
										resultMsgLabel = "naits.success.admConsole.userToGroupDetached";
										break;
									}
								}
							} else {
								throw (new SvException("naits.error.userNotLinkedToTheGroup.UserFromGroupDetachFailed",
										svr.getInstanceUser()));
							}
						} catch (SvException e) {
							log4j.info(e.getFormattedMessage());
						}
					} finally {
						if (lock != null && dboObjectToHandle != null) {
							SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
						}
					}

				}
			svw.dbCommit();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return resultMsgLabel;
	}

	public String assignOrUnasignPackagePermissions(String packageName, String actionName, JsonArray jsonData,
			String sessionId) throws Exception {
		String resultMsg = "naits.error.admConsole.permissionPackageNotAttached";
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			ArrayList<String> customPackagePermissions = new ArrayList<>();
			ArrayList<String> corePermissions = null;
			boolean containsSomePerm = false;
			boolean isSvarogReadAccess = false;
			// check if parent exist
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
							svCONST.OBJECT_TYPE_USER, null);
				}
				if (dboObjectToHandle == null && obj.get("SVAROG_USER_GROUPS.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USER_GROUPS.OBJECT_ID").getAsLong(),
							svCONST.OBJECT_TYPE_GROUP, null);
				}
				if (obj.get("object_id") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("object_id").getAsLong(), svCONST.OBJECT_TYPE_USER,
							null);
				}
				if (dboObjectToHandle == null) {
					throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					// get all custom package permissions
					// if we receive multiple package names, we can split them
					// by
					// comma (',')
					customPackagePermissions = getCustomPermissionsPerPackage(packageName, svr);
					ArrayList<String> beforeDetachPerms = getCustomPermissionForUserOrGroup(dboObjectToHandle, svr);
					// exclude existing user permissions from
					// customPackagePermissions
					// TODO
					ArrayList<String> filteredPackagePermissions = new ArrayList<>();
					ArrayList<String> currentPermissions = getCurrentPermissionsPerUserOrGroup(dboObjectToHandle, svr);
					if (actionName.equals(Tc.ASSIGN)) {
						String currPermissions = currentPermissions.toString();
						resultMsg = "naits.success.admConsole.permissionPackageSuccessfullyAttached";
						if (currentPermissions != null && !currentPermissions.isEmpty()) {
							for (String packagePerm : customPackagePermissions) {
								if (!currPermissions.contains(packagePerm)) {
									filteredPackagePermissions.add(packagePerm);
									containsSomePerm = true;
									if (filteredPackagePermissions.toString().contains("svarog_read_access")) {
										isSvarogReadAccess = true;
										DbSearchCriterion cr11 = new DbSearchCriterion(Tc.SYSTEM_TABLE,
												DbCompareOperand.EQUAL, true);
										DbDataArray dbArr = svr.getObjects(
												new DbSearchExpression().addDbSearchItem(cr11),
												svCONST.OBJECT_TYPE_TABLE, null, 0, 0);
										corePermissions = new ArrayList<>();
										for (DbDataObject tempDbo : dbArr.getItems()) {
											corePermissions.add(tempDbo.getVal(Tc.TABLE_NAME) + ".READ");
										}
									}
								}
							}
						} else {
							containsSomePerm = true;
							filteredPackagePermissions = customPackagePermissions;
						}
					} else {
						containsSomePerm = true;
						resultMsg = "naits.success.admConsole.permissionPackageSuccessfullyDetached";
						filteredPackagePermissions = customPackagePermissions;
					}
					if (!containsSomePerm) {
						throw (new SvException("naits.error.userAlreadyHasAllThePermissionsFromTheChosenPackage",
								svr.getInstanceUser()));
					}
					if (filteredPackagePermissions == null || filteredPackagePermissions.size() == 0) {
						throw (new SvException("naits.error.packagePermissionIsEmptyNothingAssigned",
								svr.getInstanceUser()));
					}
					// assign all custom package permissions
					// use always only REVOKE false, so no
					// corePermissions needs to be added
					// processPermissionsToUserOrGroup(actionName, dboObjectToHandle,
					// corePermissions, filteredPackagePermissions,
					// false, svr, svw, svs);
					if (isSvarogReadAccess) {
						processPermissionsToUserOrGroup(Tc.UNASSIGN, dboObjectToHandle, currentPermissions, null, true,
								svr, svw, svs);
					}
					processPermissionsToUserOrGroup(Tc.ASSIGN, dboObjectToHandle, corePermissions,
							filteredPackagePermissions, false, svr, svw, svs);
					if (actionName.equals(Tc.UNASSIGN)) {
						ArrayList<String> afterDetachPerms = getCustomPermissionForUserOrGroup(dboObjectToHandle, svr);
						for (String cpp : customPackagePermissions) {
							if (afterDetachPerms.contains(cpp)) {
								throw (new SvException("naits.error.permissionPackageCannotBeUnassignedGroupPermission",
										svr.getInstanceUser()));
							}
						}
						if (beforeDetachPerms.equals(afterDetachPerms)) {
							throw (new SvException("naits.error.noPackagePermissionWasUnassigned",
									svr.getInstanceUser()));
						}
					}
				} finally {
					if (lock != null && dboObjectToHandle != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svw.dbCommit();
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return resultMsg;
	}

	public ArrayList<String> getSvarogCoreTablePermission(SvReader svr) throws SvException {
		ArrayList<String> corePermissions = new ArrayList<String>();
		DbSearchCriterion cr11 = new DbSearchCriterion(Tc.SYSTEM_TABLE, DbCompareOperand.EQUAL, true);
		// DbSearchCriterion cr11 = new DbSearchCriterion(Tc.TABLE_NAME,
		// DbCompareOperand.LIKE, Tc.SVAROG);
		DbDataArray dbArray1 = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr11), svCONST.OBJECT_TYPE_TABLE,
				null, 0, 0);
		for (DbDataObject tempDbo : dbArray1.getItems()) {
			corePermissions.add(tempDbo.getVal(Tc.TABLE_NAME) + ".FULL");
		}
		return corePermissions;
	}

	public ArrayList<String> getSvarogCustomTablePermission(String permType, SvReader svr) throws SvException {
		ArrayList<String> corePermissions = new ArrayList<String>();
		if (permType.equals(Tc.READ) || permType.equals(Tc.FULL)) {
			DbSearchCriterion cr11 = new DbSearchCriterion(Tc.SYSTEM_TABLE, DbCompareOperand.EQUAL, false);
			// DbSearchCriterion cr11 = new DbSearchCriterion(Tc.TABLE_NAME,
			// DbCompareOperand.LIKE, Tc.SVAROG);
			DbDataArray dbArray1 = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr11),
					svCONST.OBJECT_TYPE_TABLE, null, 0, 0);
			for (DbDataObject tempDbo : dbArray1.getItems()) {
				corePermissions.add(tempDbo.getVal(Tc.TABLE_NAME) + "." + permType);
			}
		}
		return corePermissions;
	}

	public ArrayList<String> getCustomPermissionsPerPackage(String permissionPackage, SvReader svr) throws SvException {
		ArrayList<String> allPermPackages = loadPermissionPackages();
		HashSet<String> hs = new HashSet<>();
		String packages[] = permissionPackage.split(",");
		if (packages.length > 0) {
			for (String packageName : packages) {
				for (String tempPermissionPackage : allPermPackages) {
					String tempData[] = tempPermissionPackage.split(":");
					String tempPermissionPackageName = tempData[0];
					if (tempPermissionPackageName.equals(packageName.trim())) {
						hs.addAll(new ArrayList<>(Arrays.asList(tempData[1].split(","))));
						break;
					}
				}
			}
		}
		ArrayList<String> permissionsPerPackageName = new ArrayList<>(hs);
		return permissionsPerPackageName;
	}

	/**
	 * Help method for attaching already extracted permissions on a group
	 * 
	 * @param groupName
	 * @param customTablePermissions
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public void attachPermissionsToUserGroup(String permissionPackageName, DbDataObject groupObj,
			ArrayList<String> coreTablePermissions, ArrayList<String> customTablePermissions, String sessionId)
			throws SvException {
		SvReader svr = null;
		SvWriter svw = null;
		SvSecurity svs = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svs = new SvSecurity(svw);
			svs.setAutoCommit(false);
			svw.setAutoCommit(false);

			if (permissionPackageName.equals("ALL.READ") || permissionPackageName.equals("ALL.FULL")) {
				revokeAllCurrentPermission(groupObj, svr, svw, svs);
				if (!coreTablePermissions.isEmpty())
					setSidPermission(groupObj.getVal(Tc.GROUP_NAME).toString(), svCONST.OBJECT_TYPE_GROUP,
							coreTablePermissions, Tc.GRANT, svr.getSessionId());
			}
			if (!customTablePermissions.isEmpty())
				setSidPermission(groupObj.getVal(Tc.GROUP_NAME).toString(), svCONST.OBJECT_TYPE_GROUP,
						customTablePermissions, Tc.GRANT, svr.getSessionId());
		} catch (SvException e) {
			log4j.error("Error occured in UserGroupAttach:" + e.getFormattedMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svs != null) {
				svs.release();
			}
		}

	}

	/**
	 * Help method for attaching already extracted permissions on a group
	 * 
	 * @param groupName
	 * @param customTablePermissions
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public void processPermissionsToUserOrGroup(String processType, DbDataObject userOrGroupObj,
			ArrayList<String> coreTablePermissions, ArrayList<String> customTablePermissions, Boolean shouldRevoke,
			SvReader svr, SvWriter svw, SvSecurity svs) throws SvException {
		String permissionAction = Tc.GRANT;
		if (shouldRevoke) {
			revokeAllCurrentPermission(userOrGroupObj, svr, svw, svs);
			svw.dbCommit();
			svs.dbCommit();
		}
		if (processType.equals(Tc.UNASSIGN)) {
			permissionAction = Tc.REVOKE;
		}
		// identify if user or group
		String userOrGroupName = Tc.USER_NAME;
		if (userOrGroupObj.getObject_type().equals(svCONST.OBJECT_TYPE_GROUP)) {
			userOrGroupName = Tc.GROUP_NAME;
		}

		if (coreTablePermissions != null && !coreTablePermissions.isEmpty()) {
			setSidPermission(userOrGroupObj.getVal(userOrGroupName).toString(), userOrGroupObj.getObject_type(),
					coreTablePermissions, permissionAction, svr.getSessionId());
			svw.dbCommit();
			svs.dbCommit();
		}
		if (customTablePermissions != null && !customTablePermissions.isEmpty()) {
			setSidPermission(userOrGroupObj.getVal(userOrGroupName).toString(), userOrGroupObj.getObject_type(),
					customTablePermissions, permissionAction, svr.getSessionId());
			svw.dbCommit();
			svs.dbCommit();
		}
	}

	/**
	 * Help method for attaching already extracted permissions on a group
	 * 
	 * @param groupName
	 * @param customTablePermissions
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public void attachPermissionsToUser(String userName, ArrayList<String> customTablePermissions, SvReader svr,
			SvWriter svw) throws SvException {
		DbDataObject dboUser = findUserOrUserGroup(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, userName, svr);
		if (dboUser != null) {
			if (setSidPermission(dboUser.getVal(Tc.USER_NAME).toString(), svCONST.OBJECT_TYPE_USER,
					customTablePermissions, Tc.GRANT, svr.getSessionId()))
				log4j.info("Custom permissions granted.");
			else
				log4j.info("Custom permissions not granted.");
		}
	}

	/**
	 * Help method for revoking already extracted permissions on a group
	 * 
	 * @param groupObj
	 * @param svr
	 * @param svw
	 * @throws SvException
	 */
	public void revokeAllCurrentPermission(DbDataObject objToGetPermissionsFrom, SvReader svr, SvWriter svw,
			SvSecurity svs) throws SvException {
		if (objToGetPermissionsFrom.getObject_type().equals(svCONST.OBJECT_TYPE_GROUP)) {
			DbDataObject groupObj = objToGetPermissionsFrom;
			if (groupObj.getVal(Tc.GROUP_NAME) != null) {
				ArrayList<String> currentPermissions = getCurrentGroupPermissions(
						groupObj.getVal(Tc.GROUP_NAME).toString(), svr);
				if (setSidPermission(groupObj.getVal(Tc.GROUP_NAME).toString(), svCONST.OBJECT_TYPE_GROUP,
						currentPermissions, Tc.REVOKE, svw, svs))
					log4j.info("All permissions for:" + groupObj.getVal(Tc.GROUP_NAME).toString() + " revoked.");
				else
					log4j.info("NONE permission for:" + groupObj.getVal(Tc.GROUP_NAME).toString() + " revoked.");
			}
		}
		if (objToGetPermissionsFrom.getObject_type().equals(svCONST.OBJECT_TYPE_USER)) {
			DbDataObject userObj = objToGetPermissionsFrom;
			if (userObj.getVal(Tc.USER_NAME) != null) {
				ArrayList<String> currentPermissions = getCurrentUserPermissions(
						userObj.getVal(Tc.USER_NAME).toString(), svr);
				if (setSidPermission(userObj.getVal(Tc.USER_NAME).toString(), svCONST.OBJECT_TYPE_USER,
						currentPermissions, Tc.REVOKE, svw, svs))
					log4j.info("All permissions for:" + userObj.getVal(Tc.USER_NAME).toString() + " revoked.");
				else
					log4j.info("NONE permission for:" + userObj.getVal(Tc.USER_NAME).toString() + " revoked.");
			}
		}

	}

	public ArrayList<String> getCurrentGroupPermissions(String groupName, SvReader svr) {
		DbDataArray currentGroupPrmissions = new DbDataArray();
		ArrayList<String> result = new ArrayList<>();
		try {
			DbDataObject sid = getGroupSid(groupName, svr);
			if (sid != null) {
				currentGroupPrmissions = svr.getPermissions(sid, svr);
				if (currentGroupPrmissions != null && !currentGroupPrmissions.getItems().isEmpty()) {
					for (DbDataObject tempDbo : currentGroupPrmissions.getItems()) {
						if (tempDbo.getVal(Tc.LABEL_CODE) != null)
							result.add(tempDbo.getVal(Tc.LABEL_CODE).toString());
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return result;
	}

	public ArrayList<String> getCurrentUserPermissions(String userName, SvReader svr) {
		DbDataArray currentUserPrmissions = new DbDataArray();
		ArrayList<String> result = new ArrayList<>();
		try {
			DbDataObject sid = getUserSid(userName, svr);
			if (sid != null) {
				currentUserPrmissions = svr.getPermissions(sid, svr);
				if (currentUserPrmissions != null && !currentUserPrmissions.getItems().isEmpty()) {
					for (DbDataObject tempDbo : currentUserPrmissions.getItems()) {
						if (tempDbo.getVal(Tc.LABEL_CODE) != null)
							result.add(tempDbo.getVal(Tc.LABEL_CODE).toString());
					}
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		}
		return result;
	}

	public DbDataObject getGroupSid(String groupName, SvReader svr) {
		SvSecurity svs = null;
		DbDataObject sid = null;
		try {
			svs = new SvSecurity(svr);
			sid = svs.getSid(groupName, svCONST.OBJECT_TYPE_GROUP);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svs != null)
				svs.release();
		}
		return sid;
	}

	public DbDataObject getUserSid(String userName, SvReader svr) {
		SvSecurity svs = null;
		DbDataObject sid = null;
		try {
			svs = new SvSecurity(svr);
			sid = svs.getSid(userName, svCONST.OBJECT_TYPE_USER);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} finally {
			if (svs != null)
				svs.release();
		}
		return sid;
	}

	/**
	 * Help method to check if a group attached, really exist
	 * 
	 * @param objToSearchIn
	 * @param columnToCompare
	 * @param valueToCompare
	 * @param svr
	 * @return
	 * @throws SvException
	 */
	public DbDataObject findUserOrUserGroup(Long objToSearchIn, String columnToCompare, String valueToCompare,
			SvReader svr) throws SvException {
		Reader rdr = new Reader();
		DbDataObject result = rdr.searchForObject(objToSearchIn, columnToCompare, valueToCompare, svr);
		return result;
	}

	/**
	 * Help method to attach permissions
	 * 
	 * @param sidName
	 * @param sidType
	 * @param permissionKeys
	 * @param operation
	 * @param token
	 * @return
	 */
	public boolean setSidPermission(String sidName, Long sidType, ArrayList<String> permissionKeys, String operation,
			SvWriter svw, SvSecurity svs) {
		DbDataObject sid = null;
		try {
			sid = svs.getSid(sidName, sidType);
			for (String permissionKey : permissionKeys)
				if (operation.equals(Tc.GRANT))
					svs.grantPermission(sid, permissionKey.trim());
				else if (operation.equals(Tc.REVOKE))
					svs.revokePermission(sid, permissionKey.trim());
				else
					log4j.info("Operation must be either GRANT or REVOKE, Wrong operation:" + operation);
		} catch (SvException e) {
			return false;
		}
		return true;
	}

	/**
	 * Help method to attach permissions
	 * 
	 * @param sidName
	 * @param sidType
	 * @param permissionKeys
	 * @param operation
	 * @param token
	 * @return
	 */
	public boolean setSidPermission(String sidName, Long sidType, ArrayList<String> permissionKeys, String operation,
			String token) {
		SvSecurity svs = null;
		SvReader svr = null;
		DbDataObject sid = null;
		try {
			svr = new SvReader(token);
			// svr.switchUser("ADMIN");
			svs = new SvSecurity(svr);
			svs.setAutoCommit(false);
			sid = svs.getSid(sidName, sidType);
			for (String permissionKey : permissionKeys)
				if (operation.equals(Tc.GRANT))
					svs.grantPermission(sid, permissionKey.trim());
				else if (operation.equals(Tc.REVOKE))
					svs.revokePermission(sid, permissionKey.trim());
				else
					log4j.info("Operation must be either GRANT or REVOKE, Wrong operation:" + operation);
			svs.dbCommit();
		} catch (SvException e) {
			return false;
		} finally {
			if (svs != null)
				svs.release();
			if (svr != null)
				svr.release();
		}
		return true;
	}

	public DbDataObject createDefaultTestUser(String userName, String firstName, String lastName, String email,
			String pin, String password, SvReader svr) throws SvException {
		DbDataObject dboUser = findUserOrUserGroup(svCONST.OBJECT_TYPE_USER, Tc.USER_NAME, userName, svr);
		if (dboUser == null) {
			dboUser = new DbDataObject();
			dboUser.setObject_type(svCONST.OBJECT_TYPE_USER);
			dboUser.setVal(Tc.USER_TYPE, "INTERNAL");
			dboUser.setVal(Tc.USER_UID, UUID.randomUUID().toString());
			dboUser.setVal(Tc.USER_NAME, userName);
			dboUser.setVal(Tc.FIRST_NAME, firstName);
			dboUser.setVal(Tc.LAST_NAME, lastName);
			dboUser.setVal(Tc.PIN, pin);
			dboUser.setVal(Tc.E_MAIL, email);
			dboUser.setVal(Tc.PASS_HSH, SvUtil.getMD5(password));
		} else {
			throw (new SvException("naits.error.createUser.userAlreadyExist", svr.getInstanceUser()));
		}

		return dboUser;
	}

	public Boolean checkIfUserHasAdmGroup(DbDataObject dboUser, SvReader svr) throws SvException {
		boolean result = false;
		DbDataArray allDefaultGroups = svr.getAllUserGroups(dboUser, true);
		DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboUser, false);
		ArrayList<DbDataObject> convertedAllDefaultGroups = allDefaultGroups.getItems();
		ArrayList<DbDataObject> convertedAllAdditionalGroups = allAdditionalGroups.getItems();
		if (!convertedAllDefaultGroups.isEmpty()) {
			for (DbDataObject tempDefaultGroup : convertedAllDefaultGroups) {
				if ((tempDefaultGroup.getVal(Tc.GROUP_TYPE) != null
						&& tempDefaultGroup.getVal(Tc.GROUP_TYPE).toString().equals(Tc.ADMINISTRATORS))
						|| (tempDefaultGroup.getVal(Tc.GROUP_NAME) != null
								&& tempDefaultGroup.getVal(Tc.GROUP_NAME).toString().equals(Tc.ADMINISTRATORS))) {
					result = true;
					break;
				}
			}
		}
		if (!result && !convertedAllAdditionalGroups.isEmpty()) {
			for (DbDataObject tempAdditionalGroup : convertedAllAdditionalGroups) {
				if ((tempAdditionalGroup.getVal(Tc.GROUP_TYPE) != null
						&& tempAdditionalGroup.getVal(Tc.GROUP_TYPE).toString().equals(Tc.ADMINISTRATORS))
						|| (tempAdditionalGroup.getVal(Tc.GROUP_NAME) != null
								&& tempAdditionalGroup.getVal(Tc.GROUP_NAME).toString().equals(Tc.ADMINISTRATORS))) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Help method for fetching already extracted permissions on a user or group
	 * 
	 * @param userOrGroupObject
	 * @param svr
	 * @throws SvException
	 */
	public ArrayList<String> getCurrentPermissionsPerUserOrGroup(DbDataObject objToGetPermissionsFrom, SvReader svr)
			throws SvException {
		ArrayList<String> currentPermissions = new ArrayList<String>();
		if (objToGetPermissionsFrom.getObject_type().equals(svCONST.OBJECT_TYPE_GROUP)) {
			DbDataObject groupObj = objToGetPermissionsFrom;
			if (groupObj.getVal(Tc.GROUP_NAME) != null) {
				currentPermissions = getCurrentGroupPermissions(groupObj.getVal(Tc.GROUP_NAME).toString(), svr);
			}
		}
		if (objToGetPermissionsFrom.getObject_type().equals(svCONST.OBJECT_TYPE_USER)) {
			DbDataObject userObj = objToGetPermissionsFrom;
			if (userObj.getVal(Tc.USER_NAME) != null) {
				currentPermissions = getCurrentUserPermissions(userObj.getVal(Tc.USER_NAME).toString(), svr);
			}
		}
		return currentPermissions;
	}

	/**
	 * Help method for fetching already extracted permissions on a user or group
	 * 
	 * @param userOrGroupObject
	 * @param svr
	 * @throws SvException
	 */
	public ArrayList<String> getCustomPermissionForUserOrGroup(DbDataObject objToGetPermissionsFrom, SvReader svr)
			throws SvException {
		ArrayList<String> result = new ArrayList<>();
		ArrayList<String> tempResult = getCurrentPermissionsPerUserOrGroup(objToGetPermissionsFrom, svr);
		for (int i = 0; i < tempResult.size(); i++) {
			if (!tempResult.get(i).toUpperCase().contains(Tc.SVAROG)
					&& !tempResult.get(i).toUpperCase().contains(Tc.SYSTEM)) {
				result.add(tempResult.get(i));
			}
		}
		result.sort(null);
		return result;
	}

	/**
	 * Method that attaches Organizational unit to User or User group. If the
	 * 
	 * @param orgUnitId
	 * @param jsonData
	 * @param sessionId
	 * @return
	 * @throws Exception
	 */
	public String attachUserToOrgUnit(Long orgUnitId, JsonArray jsonData, String sessionId) throws Exception {
		String result = "naits.error.admConsole.userToOrgUnirAttached";
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Reader rdr = null;
		Writer wr = null;
		ArrayList<String> userGroupInformation = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			rdr = new Reader();
			wr = new Writer();
			String localeId = svr.getUserLocaleId(svr.getInstanceUser());
			userGroupInformation = new ArrayList<>();
			boolean isGroupType = false;
			boolean isHeadquarter = false;
			DbDataObject dboOrgUnit = null;
			if (orgUnitId.equals(0L)) {
				dboOrgUnit = rdr.searchForObject(svCONST.OBJECT_TYPE_ORG_UNITS, Tc.ORG_UNIT_TYPE, Tc.HEADQUARTER, svr);
				isHeadquarter = true;
			} else {
				dboOrgUnit = rdr.getOrgUnitByExternalId(orgUnitId, svr);
			}
			if (dboOrgUnit == null) {
				throw (new SvException("naits.error.orgUnitNotFound", svr.getInstanceUser()));
			}
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get(Tc.SVAROG_USERS + "." + Tc.OBJECT_ID) != null) {
					dboObjectToHandle = svr.getObjectById(obj.get(Tc.SVAROG_USERS + "." + Tc.OBJECT_ID).getAsLong(),
							svCONST.OBJECT_TYPE_USER, null);
				}
				if (dboObjectToHandle == null) {
					dboObjectToHandle = svr.getObjectById(
							obj.get(Tc.SVAROG_USER_GROUPS + "." + Tc.OBJECT_ID).getAsLong(), svCONST.OBJECT_TYPE_GROUP,
							null);
					isGroupType = true;
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					try {
						DbDataObject dbLinkType = SvLink.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER,
								dboOrgUnit.getObject_type());
						if (isGroupType) {
							userGroupInformation = wr.linkSubOrgUnitsWithUserOrUserGroup(dboObjectToHandle,
									dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString(), true, localeId, dbLinkType, rdr, svl,
									svr);
						} else {
							if (isHeadquarter) {
								result = wr.createLinkBetweenUserAndOrgUnitHierarchically(dboObjectToHandle, dboOrgUnit,
										dbLinkType, rdr, localeId, svw, svr);
							} else {
								wr.linkSubOrgUnitsWithUserOrUserGroup(dboObjectToHandle,
										dboOrgUnit.getVal(Tc.EXTERNAL_ID).toString(), false, localeId, dbLinkType, rdr,
										svl, svr);
							}
						}
					} catch (SvException e) {
						log4j.info(e.getFormattedMessage());
					}
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			if (isGroupType) {
				result = I18n.getText(localeId, "naits.success.attachedUserToOrgUnit");
				result = userGroupInformation.toString();
			} else {
				if (!isHeadquarter) {
					result = I18n.getText(localeId, "naits.success.attachedUserToOrgUnit");
				}
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svl != null) {
				svl.release();
			}
		}
		return result;
	}

	public String detachUsersToOrgUnit(Long orgUnitId, JsonArray jsonData, String sessionId) throws Exception {
		String result = "naits.error.admConsole.userToOrgUnitDetach";
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Reader rdr = null;
		Writer wr = null;
		ArrayList<String> usersNotLinkedToOrgUnit = null;
		ArrayList<String> userGroupInformation = null;
		boolean isGroupType = false;
		boolean isHeadquarter = false;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svw.setAutoCommit(false);
			svl = new SvLink(svw);
			rdr = new Reader();
			wr = new Writer();
			usersNotLinkedToOrgUnit = new ArrayList<>();
			userGroupInformation = new ArrayList<>();
			String localeId = svr.getUserLocaleId(svr.getInstanceUser());
			DbDataObject dboOrgUnit = null;
			if (orgUnitId.equals(0L)) {
				dboOrgUnit = rdr.searchForObject(svCONST.OBJECT_TYPE_ORG_UNITS, Tc.ORG_UNIT_TYPE, Tc.HEADQUARTER, svr);
				isHeadquarter = true;
			} else {
				dboOrgUnit = rdr.getOrgUnitByExternalId(orgUnitId, svr);
			}
			if (dboOrgUnit == null) {
				throw (new SvException("naits.error.orgUnitNotFound", svr.getInstanceUser()));
			}
			DbDataArray subOrgUnits = new DbDataArray();
			// check if parent exist
			for (int i = 0; i < jsonData.size(); i++) {
				JsonObject obj = jsonData.get(i).getAsJsonObject();
				DbDataObject dboObjectToHandle = null;
				if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
					dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
							svCONST.OBJECT_TYPE_USER, null);
				}
				if (dboObjectToHandle == null) {
					dboObjectToHandle = svr.getObjectById(
							obj.get(Tc.SVAROG_USER_GROUPS + "." + Tc.OBJECT_ID).getAsLong(), svCONST.OBJECT_TYPE_GROUP,
							null);
					isGroupType = true;
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
				}
				ReentrantLock lock = null;
				try {
					lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
					if (lock == null) {
						throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
					}
					try {
						if (isGroupType) {
							int countSuccessfullyDetachedUsers = 0;
							// informations for the user group
							userGroupInformation
									.add(dboObjectToHandle.getVal(Tc.GROUP_NAME) != null
											? I18n.getText(localeId, "naits.info.user_group_name")
													+ dboObjectToHandle.getVal(Tc.GROUP_NAME).toString()
											: Tc.NOT_AVAILABLE_NA);
							// we fetch only additional users with this link
							// since default users will be only from type users
							// and
							// administrators
							DbDataObject dboLinkBetweenUserAndUserGroup = SvReader.getLinkType(Tc.USER_GROUP,
									svCONST.OBJECT_TYPE_USER, dboObjectToHandle.getObject_type());
							DbDataArray arrayAttachedUsers = svr.getObjectsByLinkedId(dboObjectToHandle.getObject_id(),
									dboObjectToHandle.getObject_type(), dboLinkBetweenUserAndUserGroup,
									svCONST.OBJECT_TYPE_USER, true, null, 0, 0);
							userGroupInformation.add(I18n.getText(localeId, "naits.info.user_group_total_users")
									+ String.valueOf(arrayAttachedUsers.size()));
							if (!arrayAttachedUsers.getItems().isEmpty()) {
								DbDataObject dbLinkType = SvLink.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER,
										dboOrgUnit.getObject_type());
								for (DbDataObject dboUser : arrayAttachedUsers.getItems()) {
									subOrgUnits = rdr.getAppropriateSubOrgUnits(dboUser, dboOrgUnit, svr);
									if (!subOrgUnits.getItems().isEmpty()) {
										for (DbDataObject dboSubOrgUnit : subOrgUnits.getItems()) {
											DbDataObject dbLinkObj = rdr.getLinkObject(dboUser.getObject_id(),
													dboSubOrgUnit.getObject_id(), dbLinkType.getObject_id(), svr);
											if (dbLinkObj != null) {
												linkDelete(dboUser, dboSubOrgUnit, Tc.POA, svr, svw);
												if (dboSubOrgUnit.getObject_id().equals(dboOrgUnit.getObject_id())) {
													countSuccessfullyDetachedUsers++;
												}
											}
										}
									}
								}
							}
							userGroupInformation.add(I18n.getText(localeId, "naits.info.user_group_processed_users")
									+ String.valueOf(countSuccessfullyDetachedUsers));
						} else {
							DbDataObject dbLinkType = SvLink.getLinkType(Tc.POA, dboObjectToHandle.getObject_type(),
									dboOrgUnit.getObject_type());
							if (isHeadquarter) {
								result = wr.removeLinkBetweenUserAndOrgUnitHierarchically(dboObjectToHandle, dboOrgUnit,
										dbLinkType, rdr, localeId, svw, svr);
							} else {
								subOrgUnits = rdr.getAppropriateSubOrgUnits(dboObjectToHandle, dboOrgUnit, svr);
								if (!subOrgUnits.getItems().isEmpty()) {
									for (DbDataObject dboSubOrgUnit : subOrgUnits.getItems()) {
										DbDataObject dbLinkObj = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
												dboSubOrgUnit.getObject_id(), dbLinkType.getObject_id(), svr);
										if (dbLinkObj != null) {
											linkDelete(dboObjectToHandle, dboSubOrgUnit, Tc.POA, svr, svw);
										} else {
											if (dboSubOrgUnit.getObject_id().equals(dboOrgUnit.getObject_id())) {
												usersNotLinkedToOrgUnit
														.add(dboObjectToHandle.getVal(Tc.USER_NAME).toString());
											}
										}
									}
								}
							}
						}
					} catch (SvException e) {
						log4j.info(e.getFormattedMessage());
					}
				} finally {
					if (lock != null) {
						SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
					}
				}
			}
			svw.dbCommit();
			if (!isHeadquarter) {
				if (usersNotLinkedToOrgUnit != null && !usersNotLinkedToOrgUnit.isEmpty()) {
					result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
							"naits.success.theseUserDoesNotBelongToTheSelectedOrgUnit") + " "
							+ usersNotLinkedToOrgUnit.toString();
				} else {
					result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
							"naits.success.detachUserToOrgUnit");
				}
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svl != null) {
				svl.release();
			}
		}
		return result;
	}

	public String assignUserToObjectWithPoa(Long objectId, JsonArray jsonData, String sessionId) throws Exception {
		String result = "naits.error.admConsole.userToLaboratoryAssigned";
		SvReader svr = null;
		SvSecurity svs = null;
		SvLink svl = null;
		Reader rdr = null;
		ArrayList<String> usersWithSameLaboratory = null;
		try {
			svr = new SvReader(sessionId);
			svs = new SvSecurity(svr);
			svl = new SvLink(svr);
			rdr = new Reader();
			usersWithSameLaboratory = new ArrayList<>();
			String objectType = "Laboratory";
			String customPoa = Tc.EMPTY_STRING;
			DbDataObject holdingObject = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.LABORATORY), null);
			if (holdingObject == null) {
				objectType = "Holding";
				customPoa = "CUSTOM_";
				holdingObject = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			}
			if (holdingObject != null)
				for (int i = 0; i < jsonData.size(); i++) {
					JsonObject obj = jsonData.get(i).getAsJsonObject();
					DbDataObject dboObjectToHandle = null;
					if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
						dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
								svCONST.OBJECT_TYPE_USER, null);
					}
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						try {
							DbDataObject dbLinkType = SvLink.getLinkType(customPoa + Tc.POA,
									dboObjectToHandle.getObject_type(), holdingObject.getObject_type());
							DbDataObject dbLinkObj = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
									holdingObject.getObject_id(), dbLinkType.getObject_id(), svr);
							if (dbLinkObj == null) {
								svl.linkObjects(dboObjectToHandle, holdingObject, customPoa + Tc.POA, "", false, true);
							} else {
								usersWithSameLaboratory.add(dboObjectToHandle.getVal(Tc.USER_NAME).toString());
							}
						} catch (SvException e) {
							log4j.info(e.getFormattedMessage());
						}
					} finally {
						if (lock != null) {
							SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
						}
					}

				}
			if (usersWithSameLaboratory != null && !usersWithSameLaboratory.isEmpty()) {
				result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
						"naits.success.theseUserAlreadyBelongsToThe" + objectType) + " "
						+ usersWithSameLaboratory.toString();
			} else {
				result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
						"naits.success.assignUserTo" + objectType);
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svl != null) {
				svl.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return result;
	}

	public String unassignUserFromObjectWithPoa(Long objectId, JsonArray jsonData, String sessionId) throws Exception {
		String result = "naits.error.admConsole.userToLaboratoryUnassigned";
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		Reader rdr = null;
		ArrayList<String> usersNotLinkedToLaboratory = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svl = new SvLink(svr);
			rdr = new Reader();
			usersNotLinkedToLaboratory = new ArrayList<>();
			String objectType = "Laboratory";
			String customPoa = Tc.EMPTY_STRING;
			DbDataObject dboToLink = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.LABORATORY), null);
			if (dboToLink == null) {
				objectType = "Holding";
				customPoa = "CUSTOM_";
				dboToLink = svr.getObjectById(objectId, SvReader.getTypeIdByName(Tc.HOLDING), null);
			}
			if (dboToLink != null)
				for (int i = 0; i < jsonData.size(); i++) {
					JsonObject obj = jsonData.get(i).getAsJsonObject();
					DbDataObject dboObjectToHandle = null;
					if (obj.get("SVAROG_USERS.OBJECT_ID") != null) {
						dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_USERS.OBJECT_ID").getAsLong(),
								svCONST.OBJECT_TYPE_USER, null);
					}
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						try {
							DbDataObject dbLinkType = SvLink.getLinkType(customPoa + Tc.POA,
									dboObjectToHandle.getObject_type(), dboToLink.getObject_type());
							DbDataObject dbLinkObj = rdr.getLinkObject(dboObjectToHandle.getObject_id(),
									dboToLink.getObject_id(), dbLinkType.getObject_id(), svr);
							if (dbLinkObj != null) {
								linkDelete(dboObjectToHandle, dboToLink, customPoa + Tc.POA, svr, svw);
							} else {
								usersNotLinkedToLaboratory.add(dboObjectToHandle.getVal(Tc.USER_NAME).toString());
							}
						} catch (SvException e) {
							log4j.info(e.getFormattedMessage());
						}
					} finally {
						if (lock != null) {
							SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
						}
					}

				}
			if (usersNotLinkedToLaboratory != null && !usersNotLinkedToLaboratory.isEmpty()) {
				result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
						"naits.success.theseUserDoesNotBelongToTheSelected" + objectType) + " "
						+ usersNotLinkedToLaboratory.toString();
			} else {
				result = I18n.getText(svr.getUserLocaleId(svr.getInstanceUser()),
						"naits.success.unassignUserFrom" + objectType);
			}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svl != null) {
				svl.release();
			}
			if (svw != null) {
				svw.release();
			}
		}
		return result;
	}

	public void detachUserToOrgUnis(Long userId, JsonArray jsonData, String sessionId) throws Exception {
		SvReader svr = null;
		SvWriter svw = null;
		SvLink svl = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			DbDataObject dboUser = svr.getObjectById(userId, svCONST.OBJECT_TYPE_USER, null);

			if (dboUser != null)
				// check if parent exist
				for (int i = 0; i < jsonData.size(); i++) {
					JsonObject obj = jsonData.get(i).getAsJsonObject();
					DbDataObject dboObjectToHandle = null;
					if (obj.get("SVAROG_ORG_UNITS.OBJECT_ID") != null) {
						dboObjectToHandle = svr.getObjectById(obj.get("SVAROG_ORG_UNITS.OBJECT_ID").getAsLong(),
								svCONST.OBJECT_TYPE_ORG_UNITS, null);
					}
					if (dboObjectToHandle == null) {
						throw (new SvException("naits.error.obj_not_found", svr.getInstanceUser()));
					}
					ReentrantLock lock = null;
					try {
						lock = SvLock.getLock(String.valueOf(dboObjectToHandle.getObject_id()), false, 0);
						if (lock == null) {
							throw (new SvException("naits.error.objectUsedByOtherSession", svr.getInstanceUser()));
						}
						try {
							linkDelete(dboUser, dboObjectToHandle, Tc.POA, svr, svw);
						} catch (SvException e) {
							log4j.info(e.getFormattedMessage());
						}
					} finally {
						if (lock != null) {
							SvLock.releaseLock(String.valueOf(dboObjectToHandle.getObject_id()), lock);
						}
					}

				}
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svw != null) {
				svw.release();
			}
			if (svl != null) {
				svl.release();
			}
		}
	}

	public void linkDelete(DbDataObject obj1, DbDataObject obj2, String linkType, SvReader svr, SvWriter svw)
			throws SvException {
		DbDataObject dblt = SvLink.getLinkType(linkType, obj1.getObject_type(), obj2.getObject_type());
		DbSearchExpression exp = new DbSearchExpression();
		exp.addDbSearchItem(new DbSearchCriterion(Tc.LINK_TYPE_ID, DbCompareOperand.EQUAL, dblt.getObject_id()));
		exp.addDbSearchItem(new DbSearchCriterion(Tc.LINK_OBJ_ID_1, DbCompareOperand.EQUAL, obj1.getObject_id()))
				.addDbSearchItem(new DbSearchCriterion(Tc.LINK_OBJ_ID_2, DbCompareOperand.EQUAL, obj2.getObject_id()));
		DbDataArray links = svr.getObjects(exp, svCONST.OBJECT_TYPE_LINK, null, 0, 0);
		if (!links.getItems().isEmpty()) {
			DbDataObject lnk = links.get(0);
			svw.deleteObject(lnk);
		} else
			throw (new SvException("system.error.user_not_member_of_orgUnit", svr.getInstanceUser(), obj1, obj2));
	}

	public boolean checkIfUserLinkedToDefaultGroup(DbDataObject dboUser, String userGroupName, SvReader svr)
			throws SvException {
		Boolean result = false;
		DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboUser, false);
		for (DbDataObject tempUserGroup : allAdditionalGroups.getItems()) {
			if (tempUserGroup.getVal(Tc.GROUP_NAME).toString().equals(userGroupName)) {
				result = true;
				break;
			}
		}
		/*
		 * if (!result) { DbDataArray allAdditionalGroups =
		 * svr.getAllUserGroups(dboUser, false); for (DbDataObject tempUserGroup :
		 * allAdditionalGroups.getItems()) { if
		 * (tempUserGroup.getVal(Tc.GROUP_NAME).toString().equals(userGroupName) ) {
		 * result = true; break; } } }
		 */

		return result;
	}

	public ArrayList<String> checkIfUserLinkedToSomeDefaultGroups(DbDataObject dboUser,
			ArrayList<String> userGroupsToCheck, SvReader svr) throws SvException {
		ArrayList<String> result = new ArrayList<String>();
		DbDataArray allAdditionalGroups = svr.getAllUserGroups(dboUser, false);
		for (DbDataObject tempUserGroup : allAdditionalGroups.getItems()) {
			for (String tempGroupNameToCheck : userGroupsToCheck) {
				if (tempUserGroup.getVal(Tc.GROUP_NAME).toString().equals(tempGroupNameToCheck)) {
					result.add(tempGroupNameToCheck);
					break;
				}
			}
		}
		return result;
	}

	public String createPOABetweenUserAndOrgUnit(DbDataObject dboUser, DbDataObject dboOrgUnit, SvReader svr) {
		SvLink svl = null;
		SvWriter svw = null;
		Reader rdr = null;
		String result = "naits.error.assignOrgUnitToUserNotCompleted";
		try {
			svw = new SvWriter(svr);
			svl = new SvLink(svw);
			rdr = new Reader();
			svw.setAutoCommit(false);
			DbDataObject dblink = SvCore.getLinkType(Tc.POA, svCONST.OBJECT_TYPE_USER, svCONST.OBJECT_TYPE_ORG_UNITS);
			DbDataObject linkObj = rdr.getLinkObject(dboUser.getObject_id(), dboOrgUnit.getObject_id(),
					dblink.getObject_id(), svr);
			if (linkObj != null) {
				result = "naits.error.OrgUnitAlreadyAssignedToUser";
			} else {
				svl.linkObjects(dboUser.getObject_id(), dboOrgUnit.getObject_id(), dblink.getObject_id(), null, true,
						false);
				svl.dbCommit();
				result = "naits.success.assignOrgUnitToUserCompleted";
			}
		} catch (SvException sve) {
			log4j.error(sve.getFormattedMessage(), sve);
		} finally {
			if (svw != null)
				svw.release();
			if (svl != null)
				svl.release();
		}
		return result;
	}

	public Boolean checkIfUserHasCustomPermission(DbDataObject dboUser, String permission, SvReader svr)
			throws SvException {
		boolean result = false;
		ArrayList<String> appliedPermissions = getCurrentPermissionsPerUserOrGroup(dboUser, svr);
		if (!appliedPermissions.isEmpty()) {
			for (String customPermission : appliedPermissions) {
				if (permission.equals(customPermission)) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	public Boolean checkIfUserHasCustomPermission(DbDataObject dboUser, String tableName, String access, SvReader svr)
			throws SvException {
		return checkIfUserHasCustomPermission(dboUser, tableName + "." + access, svr);
	}

	public String login(String username, String password) {
		String tokenLocal = Tc.EMPTY_STRING;
		try {
			SvSecurity svSec = new SvSecurity();
			tokenLocal = svSec.logon(username, SvUtil.getMD5(password));
		} catch (Exception e) {
			return "";
		}
		return tokenLocal;
	}

	/**
	 * Method that return all permission packages per user permission/s
	 * 
	 * @param dboUser DbDataObject of user
	 * @param svr     SvReader instance
	 * @return
	 * @throws SvException
	 */
	public ArrayList<String> getAllPermissionPackagesAccordingUserPermissions(DbDataObject dboUser, SvReader svr)
			throws SvException {
		ArrayList<String> currentPermissions = getCurrentPermissionsPerUserOrGroup(dboUser, svr);
		ArrayList<String> definiedPermissionPackages = loadPermissionPackages();
		ArrayList<String> finalPermissionPackages = new ArrayList<>();
		for (String tempDefinied : definiedPermissionPackages) {
			String permissionPackage = tempDefinied.split(":")[0];
			List<String> permissions = Arrays.asList(tempDefinied.split(":")[1].split(", "));
			boolean isPresent = true;
			for (String tempPermission : permissions) {
				if (!currentPermissions.contains(tempPermission)) {
					isPresent = false;
					break;
				}
			}
			if (isPresent) {
				finalPermissionPackages.add(permissionPackage);
			}

		}
		return finalPermissionPackages;
	}
}