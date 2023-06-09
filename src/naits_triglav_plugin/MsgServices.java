package naits_triglav_plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvSecurity;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;
import com.prtech.svarog_common.DbSearchCriterion;
import com.prtech.svarog_common.DbSearchCriterion.DbCompareOperand;
import com.prtech.svarog_common.DbSearchExpression;

import javax.ws.rs.*;

@Path("/naits_triglav_plugin/MsgServices")
public class MsgServices {
	/**
	 * Log4j instance used for logging
	 */
	static final Logger log4j = LogManager.getLogger(MsgServices.class.getName());

	/**
	 * Web service that create new message (both initial and reply)
	 * 
	 * @param sessionId
	 * @param formVals
	 * @return
	 */
	@Path("/createNewMessage/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response createNewMessage(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToCreateNewMessage";
		Writer wr = new Writer();
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);

			Long subjectObjId = Long.valueOf(formVals.get(Tc.SUBJECT_OBJ_ID).get(0));
			String subjectModuleName = formVals.get(Tc.SUBJECT_MODULE_NAME).get(0);
			String subjectTitle = formVals.get(Tc.SUBJECT_TITLE).get(0);
			String subjectPriority = formVals.get(Tc.SUBJECT_PRIORITY).get(0);
			String subjectCategory = formVals.get(Tc.SUBJECT_CATEGORY).get(0);
			String msgText = formVals.get(Tc.MSG_TEXT).get(0);
			String msgPriority = formVals.get(Tc.MSG_PRIORITY).get(0);
			String msgTo = formVals.get(Tc.MSG_TO).get(0);
			String msgCc = formVals.get(Tc.MSG_CC).get(0);
			String msgBcc = formVals.get(Tc.MSG_BCC).get(0);
			String msgAttachment = formVals.get(Tc.MSG_ATTACHMENT).get(0);
			Long orgUnitObjId = Long.valueOf(formVals.get(Tc.ORG_UNIT_OBJ_ID).get(0));

			// subject section
			DbDataObject dboSubject = null;
			if (subjectObjId == 0L) {
				dboSubject = wr.createSubject(subjectModuleName, subjectTitle, subjectCategory, subjectPriority, svw);
			} else {
				dboSubject = svr.getObjectById(subjectObjId, SvReader.getTypeIdByName(Tc.SUBJECT), null);
			}

			// message section
			DbDataObject dboMessage = wr.createMessage(msgText, msgPriority, msgTo, msgCc, msgBcc, dboSubject,
					orgUnitObjId, svw, svr);

			// attachment
			wr.processMessageAttachmentInfo(msgAttachment, dboMessage, svw, svr);
			result = "naits.success.successfullyCreatedMessage";

		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing createNewMessage:", e1);
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
	 * Web service that searches subjects by multiple criteria
	 * (title/category/priority/message text). Subject can be searched by multiple
	 * criteria at once
	 * 
	 * @param sessionId
	 * @param formVals
	 * @return
	 */
	@Path("/searchSubjects/{sessionId}")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchSubjects(@PathParam("sessionId") String sessionId, MultivaluedMap<String, String> formVals) {
		String result = "naits.error.noSubjectsHaveBeenFound";
		SvReader svr = null;
		DbDataArray dbArrSubjects = null;
		Reader rdr = null;
		DbDataArray dbArrFinal = null;
		DbDataArray dbArrMsgTo = null;
		JsonArray jsonArray = null;
		DbDataObject dboMessage = null;
		try {
			svr = new SvReader(sessionId);
			dbArrSubjects = new DbDataArray();
			rdr = new Reader();
			dbArrFinal = new DbDataArray();
			dbArrMsgTo = new DbDataArray();
			jsonArray = new JsonArray();
			dboMessage = new DbDataObject();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);

			if (dboUser != null) {
				String subjectTitleVal = formVals.get(Tc.SUBJECT_TITLE).get(0);
				String subjectCategoryVal = formVals.get(Tc.SUBJECT_CATEGORY).get(0);
				String subjectPriorityVal = formVals.get(Tc.SUBJECT_PRIORITY).get(0);
				String messageText = formVals.get(Tc.MSG_TEXT).get(0);

				// search subjects by subject/message criteria
				dbArrSubjects = rdr.getSubjectsBySubjectAndMessageCriteria(dboUser, subjectTitleVal, subjectCategoryVal,
						subjectPriorityVal, messageText, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				if (dbArrFinal != null && !dbArrFinal.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrFinal.getItems()) {
						JsonObject json = new JsonObject();
						json.add(Tc.SUBJECT, dbo.toSimpleJson());
						DbDataArray dbArrMessages = svr.getObjectsByParentId(dbo.getObject_id(),
								SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
						if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
							if (dbArrMessages.size() == 1) {
								dboMessage = dbArrMessages.get(0);
							} else if (dbArrMessages.size() > 1) {
								dboMessage = dbArrMessages.get(dbArrMessages.size() - 1);
							}
							dbArrMsgTo = rdr.getUsersLinkedToMessage(dboMessage, json, svr);
							json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
							json.add(Tc.MSG_CC, rdr.getUsersLinkedToMessage(dboMessage, Tc.MSG_CC, svr).toSimpleJson());
							json.add(Tc.MSG_BCC,
									rdr.getUsersLinkedToMessage(dboMessage, Tc.MSG_BCC, svr).toSimpleJson());
							json.addProperty(Tc.CREATED_BY_USERNAME,
									dboMessage.getVal(Tc.CREATED_BY_USERNAME).toString());
							json.addProperty(Tc.DATE_OF_CREATION, dboMessage.getDt_insert().toString());
						}
						jsonArray.add(json);
					}
				}
			}
			result = jsonArray.toString();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing searchSubjects:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that return sent subjects
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getSentSubjects/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getSentSubjects(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetSentSubjects";
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonArray jsonArray = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			jsonArray = new JsonArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrSubjects = rdr.getSentOrArchiveSubjects(dboUser, Tc.VALID, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jsonArray = rdr.getSentOrArchivedSubjectsWithMessageRecipientInfo(dbArrFinal, dboUser, svr);
				result = jsonArray.toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getSentSubjects:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that return archived subjects
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getArchivedSubjects/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getArchivedSubjects(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetArchivedSubjects";
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonArray jsonArray = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			jsonArray = new JsonArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrSubjects = rdr.getSentOrArchiveSubjects(dboUser, Tc.CLOSED, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jsonArray = rdr.getSentOrArchivedSubjectsWithMessageRecipientInfo(dbArrFinal, dboUser, svr);
				result = jsonArray.toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getArchivedSubjects:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that return subjects in inbox
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getInboxSubjects/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getInboxSubjects(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetInboxSubjects";
		Reader rdr = new Reader();
		SvReader svr = null;
		DbDataArray dbArrMessages = null;
		DbDataArray dbArrSubjects = null;
		DbDataArray dbArrFinal = null;
		JsonArray jsonArray = null;
		try {
			svr = new SvReader(sessionId);
			dbArrMessages = new DbDataArray();
			dbArrSubjects = new DbDataArray();
			dbArrFinal = new DbDataArray();
			jsonArray = new JsonArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrMessages = rdr.getMessagesByCriteria(dboUser, svr);
				dbArrSubjects = rdr.getSubjectsInArray(dbArrMessages, svr);
				dbArrFinal = rdr.removeDuplicatesFromDbDataArray(dbArrSubjects);
				jsonArray = rdr.getInboxSubjectsWithMessageRecipientInfo(dbArrFinal, dboUser, svr);
				result = jsonArray.toString();
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getInboxSubjects:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Web service that change status of subject to VALID. Automatic message is sent
	 * to all users in that subjects when the status is changed to CLOSED
	 * 
	 * @param sessionId
	 * @param subjectObjId
	 * @return
	 */
	@Path("/archiveSubject/{sessionId}/{subjectObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response archiveSubject(@PathParam("sessionId") String sessionId,
			@PathParam("subjectObjId") Long subjectObjId) {
		String result = "naits.error.failedToChangeSubjectStatus";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboSubject = svr.getObjectById(subjectObjId, SvReader.getTypeIdByName(Tc.SUBJECT), null);
			if (dboUser != null && dboSubject != null) {
				if (dboSubject.getStatus().equals(Tc.VALID)) {
					dboSubject.setStatus(Tc.CLOSED);
					svw.saveObject(dboSubject);
					svw.dbCommit();
					result = "naits.success.successfullyChangedSubjectStatus";
					wr.sendAutomaticMessage(dboSubject, svw, svr);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing archiveSubject:", e1);
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
	 * Web service that change status of subject from CLOSED to VALID
	 * 
	 * @param sessionId
	 * @param subjectObjId
	 * @return
	 */
	@Path("/unArchiveSubject/{sessionId}/{subjectObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response unArchiveSubject(@PathParam("sessionId") String sessionId,
			@PathParam("subjectObjId") Long subjectObjId) {
		String result = "naits.error.failedToChangeSubjectStatus";
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboSubject = svr.getObjectById(subjectObjId, SvReader.getTypeIdByName(Tc.SUBJECT), null);
			if (dboUser != null && dboSubject != null) {
				if (dboSubject.getStatus().equals(Tc.CLOSED)) {
					dboSubject.setStatus(Tc.VALID);
					svw.saveObject(dboSubject);
					svw.dbCommit();
					result = "naits.success.successfullyChangedSubjectStatus";
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing unArchiveSubject:", e1);
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
	 * Web service that updates the status of link between message and user to VALID
	 * 
	 * @param sessionId
	 * @param messageObjId
	 * @return
	 */
	@Path("/updateStatusOfLinkBetweenMessageAndUser/{sessionId}/{messageObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response updateStatusOfLinkBetweenMessageAndUser(@PathParam("sessionId") String sessionId,
			@PathParam("messageObjId") Long messageObjId) {
		String result = "naits.error.failedToChangeStatusOfLink";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null && messageObjId != null) {
				if (wr.updateStatusOfLinkBetweenMessageAndUser(dboUser, messageObjId, svw, svr)) {
					result = "naits.success.successfullyUpdatedStatusOfLink";
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing updateStatusOfLinkBetweenMessageAndUser:", e1);
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
	 * Web service that return the number of unread messages per user
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getNumberOfUnreadMessagesPerUser/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getNumberOfUnreadMessagesPerUser(@PathParam("sessionId") String sessionId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				result = String.valueOf(rdr.getNumberOfUnreadMessagesPerUser(dboUser, svr));
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getNumberOfUnreadMessagesPerUser:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	/**
	 * Method that return number of unread inbox & archived subjects per user
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getNumberOfUnreadInboxAndArchivedMessagesPerUser/{sessionId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNumberOfUnreadInboxAndArchivedMessagesPerUser(@PathParam("sessionId") String sessionId) {
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArrUnreadMessages = null;
		int counterValid = 0;
		int counterClosed = 0;
		JsonObject jsonObject = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			jsonObject = new JsonObject();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				dbArrUnreadMessages = rdr.getListOfUnreadMessagesPerUser(dboUser, svr);
				if (dbArrUnreadMessages != null && !dbArrUnreadMessages.getItems().isEmpty()) {
					for (DbDataObject dboTemp : dbArrUnreadMessages.getItems()) {
						DbDataObject dboMessage = svr.getObjectById((Long) dboTemp.getVal(Tc.LINK_OBJ_ID_1),
								SvReader.getTypeIdByName(Tc.MESSAGE), null);
						DbDataObject dboSubject = svr.getObjectById(dboMessage.getParent_id(),
								SvReader.getTypeIdByName(Tc.SUBJECT), null);
						switch (dboSubject.getStatus()) {
						case Tc.VALID:
							counterValid++;
							break;
						case Tc.CLOSED:
							counterClosed++;
							break;
						default:
							break;
						}
					}
				}
				jsonObject.addProperty(Tc.NUMBER_OF_UNREAD_SUBJECTS_INBOX, counterValid);
				jsonObject.addProperty(Tc.NUMBER_OF_UNREAD_SUBJECTS_ARCHIVED, counterClosed);
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getNumberOfUnreadInboxAndArchivedMessagesPerUser:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jsonObject.toString()).build();
	}

	/**
	 * Web service that return org unit per org unit type
	 * 
	 * @param sessionId
	 * @param orgUnitType Org unit type (REGIONAL_OFFICE/MINICIPALITY_OFFICE)
	 * @return
	 */
	@Path("/getOrgUnitPerOrgUnitType/{sessionId}/{orgUnitType}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getOrgUnitPerOrgUnitType(@PathParam("sessionId") String sessionId,
			@PathParam("orgUnitType") String orgUnitType) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		SvSecurity svs = null;
		Reader rdr = null;
		DbDataArray dbArrOrgUnits = null;
		try {
			svs = new SvSecurity();
			svs.switchUser(svCONST.serviceUser);
			svr = new SvReader(svs);
			rdr = new Reader();
			dbArrOrgUnits = new DbDataArray();
			dbArrOrgUnits = rdr.searchForOrgUnit(orgUnitType, svr);
			if (dbArrOrgUnits != null && !dbArrOrgUnits.getItems().isEmpty()) {
				result = rdr.convertDbDataArrayToGridJson(dbArrOrgUnits, Tc.SVAROG_ORG_UNITS, false, Tc.PKID, Tc.DESC,
						svr);
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				if (!((SvException) e).getLabelCode().equals("system.error.cant_switch_system_user"))
					;
			}
			log4j.error("General error in processing getOrgUnitPerOrgUnitType:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
			if (svs != null) {
				svs.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	// NOT IN USE FOR NOW
	@Path("/getInboxMessages/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getInboxMessages(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetInboxMessages";
		Reader rdr = new Reader();
		SvReader svr = null;
		DbDataArray finalMessageList = new DbDataArray();
		DbDataArray directMessages = new DbDataArray();
		try {
			svr = new SvReader(sessionId);
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				// get directly assigned messages
				DbSearchCriterion cr1 = new DbSearchCriterion(Tc.ASSIGNED_TO, DbCompareOperand.EQUAL,
						dboUser.getObject_id());
				DbSearchCriterion cr2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.VALID);
				directMessages = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
						SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
				if (!directMessages.getItems().isEmpty()) {
					finalMessageList = directMessages;
				}
				// get messages through link
				rdr.getInboxMessagesThroughLink(dboUser, finalMessageList, svr);
			}
			result = rdr.convertDbDataArrayToGridJson(finalMessageList, Tc.MESSAGE, false, Tc.PKID, Tc.DESC, svr);
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getInboxMessages:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getSentMessages/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getSentMessages(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetSentMessages";
		SvReader svr = null;
		DbDataArray dbArrMessages = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			dbArrMessages = new DbDataArray();
			rdr = new Reader();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				DbSearchCriterion cr1 = new DbSearchCriterion(Tc.CREATED_BY, DbCompareOperand.EQUAL,
						dboUser.getObject_id());
				DbSearchCriterion cr2 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.VALID);
				dbArrMessages = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1).addDbSearchItem(cr2),
						SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
				if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(dbArrMessages, Tc.MESSAGE, false, Tc.PKID, Tc.DESC, svr);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getSentMessages:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getAdditionalMessageInfo/{sessionId}/{msgObjId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdditionalMessageInfo(@PathParam("sessionId") String sessionId,
			@PathParam("msgObjId") Long msgObjId) {
		String result = Tc.EMPTY_STRING;
		SvReader svr = null;
		Reader rdr = null;
		JsonObject json = null;
		DbDataArray dbArrMsgTo = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			json = new JsonObject();
			dbArrMsgTo = new DbDataArray();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboMessage = svr.getObjectById(msgObjId, SvReader.getTypeIdByName(Tc.MESSAGE), null);
			if (dboUser != null && dboMessage != null) {
				if (rdr.checkIfLinkExists(dboMessage, dboUser, Tc.MSG_TO, null, svr)
						|| rdr.checkIfLinkExists(dboMessage, dboUser, Tc.MSG_BCC, null, svr)
						|| dboMessage.getVal(Tc.CREATED_BY).equals(dboUser.getObject_id())
						|| (dboMessage.getVal(Tc.ASSIGNED_TO) != null
								&& dboMessage.getVal(Tc.ASSIGNED_TO).equals(dboUser.getObject_id()))) {
					dbArrMsgTo = rdr.getUsersLinkedToMessage(dboMessage, json, svr);
					json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
					json.add(Tc.MSG_CC, rdr.getUsersLinkedToMessage(dboMessage, Tc.MSG_CC, svr).toSimpleJson());
					json.add(Tc.MSG_BCC, rdr.getUsersLinkedToMessage(dboMessage, Tc.MSG_BCC, svr).toSimpleJson());
				}
				if (rdr.checkIfLinkExists(dboMessage, dboUser, Tc.MSG_CC, null, svr)) {
					dbArrMsgTo = rdr.getUsersLinkedToMessage(dboMessage, json, svr);
					json.add(Tc.MSG_TO, dbArrMsgTo.toSimpleJson());
				}
				json.addProperty(Tc.DATE_OF_CREATION, dboMessage.getDt_insert().toString());
			}
			result = json.toString();
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getAdditionalMessageInfo:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/changeMessageStatus/{sessionId}/{msgObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response changeMessageStatus(@PathParam("sessionId") String sessionId,
			@PathParam("msgObjId") Long msgObjId) {
		String result = "naits.error.failedToChangeMessageStatus";
		SvReader svr = null;
		SvWriter svw = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			DbDataObject dboMessage = svr.getObjectById(msgObjId, SvReader.getTypeIdByName(Tc.MESSAGE), null);
			if (dboUser != null && dboMessage != null) {
				if (dboMessage.getStatus().equals(Tc.VALID)) {
					dboMessage.setStatus(Tc.CLOSED);
					svw.saveObject(dboMessage);
					svw.dbCommit();
					result = "naits.success.successfullyChangedMessageStatus";
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing changeMessageStatus:", e1);
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

	@Path("/getArchivedMessages/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getArchivedMessages(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetArchivedMessages";
		SvReader svr = null;
		DbDataArray dbArrMessages = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			dbArrMessages = new DbDataArray();
			rdr = new Reader();
			DbDataObject dboUser = SvReader.getUserBySession(sessionId);
			if (dboUser != null) {
				DbSearchCriterion cr1 = new DbSearchCriterion(Tc.STATUS, DbCompareOperand.EQUAL, Tc.CLOSED);
				dbArrMessages = svr.getObjects(new DbSearchExpression().addDbSearchItem(cr1),
						SvReader.getTypeIdByName(Tc.MESSAGE), null, 0, 0);
				if (dbArrMessages != null && !dbArrMessages.getItems().isEmpty()) {
					result = rdr.convertDbDataArrayToGridJson(dbArrMessages, Tc.MESSAGE, false, Tc.PKID, Tc.DESC, svr);
				}
			}
		} catch (SvException e) {
			log4j.error(e.getFormattedMessage(), e);
		} catch (Exception e1) {
			e1.printStackTrace();
			log4j.error("General error in processing getArchivedMessages:", e1);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}
}
