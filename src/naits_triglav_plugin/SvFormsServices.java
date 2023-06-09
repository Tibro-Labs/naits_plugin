package naits_triglav_plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.prtech.svarog.I18n;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.SvWriter;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

@Path("/naits_triglav_plugin/SvFormsServices")
public class SvFormsServices {
	static final Logger log4j = LogManager.getLogger(SvFormsServices.class.getName());

	/**
	 * Web service for customized JSON schema
	 * 
	 * @param sessionId
	 * @param questionnaireLabel
	 * @return
	 */
	@Path("/getSVFormTypeJsonSchema/{sessionId}/{questionnaireLabel}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSVFormTypeJsonSchema(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireLabel") String questionnaireLabel) {
		SvReader svr = null;
		Reader rdr = null;
		JsonObject jObjResult = null;
		Boolean isEmptyJson = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			DbDataObject formType = rdr.getSvFormTypeByTitle(questionnaireLabel, svr);
			if (formType != null) {
				DbDataArray dbArrFormFieldTypes = rdr
						.getDbArrayOfSvFormFieldTypeLinkedToSvFormType(formType.getObject_id(), svr);
				jObjResult = rdr.prepareJsonSchemaForQuestionnaire(formType, dbArrFormFieldTypes, svr);
				if (dbArrFormFieldTypes != null && !dbArrFormFieldTypes.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrFormFieldTypes.getItems()) {
						isEmptyJson = true;
						int counter = 0;
						JsonObject jsonPropeties = jObjResult.get("properties").getAsJsonObject();
						String fullLabelCodeWithPrefix = dbo.getVal(Tc.LABEL_CODE).toString();
						String labelCode = fullLabelCodeWithPrefix.substring(18, fullLabelCodeWithPrefix.length());
						JsonObject jPropObj = jsonPropeties.get(labelCode).getAsJsonObject();
						JsonObject enums = new JsonObject();
						if (formType.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.COMPLEX_QUESTIONNAIRE)) {
							String questionLabelCode = dbo.getVal(Tc.LABEL_CODE).toString();
							DbDataArray dbArrListOfQuestionOptions = rdr
									.getAllQuestionOptionsByQuestionLabelCode(questionLabelCode, svr);
							JsonArray jArrayEnums = new JsonArray();
							JsonArray jArrayEnumNames = new JsonArray();
							if (dbArrListOfQuestionOptions != null
									&& !dbArrListOfQuestionOptions.getItems().isEmpty()) {
								for (DbDataObject dboTemp : dbArrListOfQuestionOptions.getItems()) {
									counter++;
									DbDataArray dbArrSvParam = svr.getObjectsByParentId(dbo.getObject_id(),
											svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
									if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
										for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
											DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(
													dboSvParam.getObject_id(), svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0,
													0);
											if (dbArrSvParamValues != null
													&& !dbArrSvParamValues.getItems().isEmpty()) {
												DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
													isEmptyJson = false;
													enums.addProperty("type", "number");
													jArrayEnumNames.add(new JsonPrimitive(
															I18n.getText(dboTemp.getVal(Tc.CODE_VALUE).toString())));
													jArrayEnums.add(new JsonPrimitive(counter));
													enums.add("enumNames", jArrayEnumNames);
													enums.add("enum", jArrayEnums);
													jPropObj.add("items", enums);
													jPropObj.addProperty("uniqueItems", true);
												}
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
													isEmptyJson = false;
													jArrayEnumNames.add(new JsonPrimitive(
															I18n.getText(dboTemp.getVal(Tc.CODE_VALUE).toString())));
													jArrayEnums.add(new JsonPrimitive(
															I18n.getText(dboTemp.getVal(Tc.CODE_VALUE).toString())));
													jPropObj.add("enumNames", jArrayEnumNames);
													jPropObj.add("enum", jArrayEnums);
												}
											}
										}
									}
								}
							} else {
								DbDataArray dbArrSvParam = svr.getObjectsByParentId(dbo.getObject_id(),
										svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
								if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
									for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
										DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(
												dboSvParam.getObject_id(), svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
										if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
											DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
											if ((dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("long")
													|| dboSvParamValue.getVal(Tc.VALUE).toString()
															.equalsIgnoreCase("short"))
													&& (!dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")
															&& !dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")
															&& !dboSvParamValue.getVal(Tc.VALUE).toString()
																	.equals("true"))) {
												isEmptyJson = false;
											}
										}
									}
								}
							}
						}
						if (formType.getVal(Tc.FORM_CATEGORY).toString().equals(Tc.SIMPLE_QUESTIONNAIRE)) {
							isEmptyJson = false;
							JsonArray jArrayEnums = new JsonArray();
							jArrayEnums.add(new JsonPrimitive(0));
							jArrayEnums.add(new JsonPrimitive(1));
							jArrayEnums.add(new JsonPrimitive(2));
							jPropObj.add("enum", jArrayEnums);
							JsonArray jArrayEnumNames = new JsonArray();
							jArrayEnumNames.add(new JsonPrimitive(I18n.getText("naits.main.yes")));
							jArrayEnumNames.add(new JsonPrimitive(I18n.getText("naits.main.no")));
							jArrayEnumNames.add(new JsonPrimitive(I18n.getText("naits.main.not_applicable")));
							jPropObj.add("enumNames", jArrayEnumNames);
						}
					}
				}
				if (isEmptyJson) {
					jObjResult = new JsonObject();
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jObjResult.toString()).build();
	}

	/**
	 * Web service for customized UI schema
	 * 
	 * @param sessionId
	 * @param questionnaireLabelCode
	 * @return
	 */
	@Path("/getCustomFormTypeUISchema/{sessionId}/{questionnaireLabelCode}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCustomFormTypeUISchema(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireLabelCode") String questionnaireLabelCode) {
		SvReader svr = null;
		JsonObject jObjResult = null;
		Reader rdr = null;
		try {
			svr = new SvReader(sessionId);
			jObjResult = new JsonObject();
			rdr = new Reader();
			DbDataObject formType = rdr.getSvFormTypeByTitle(questionnaireLabelCode, svr);
			if (formType != null) {
				DbDataArray dbArrFormFieldTypes = rdr
						.getDbArrayOfSvFormFieldTypeLinkedToSvFormType(formType.getObject_id(), svr);
				if (dbArrFormFieldTypes != null && !dbArrFormFieldTypes.getItems().isEmpty()) {
					for (DbDataObject dbo : dbArrFormFieldTypes.getItems()) {
						JsonObject jsonOptions = new JsonObject();
						jsonOptions.addProperty(Tc.inline, true);
						JsonObject jsonUISchema = new JsonObject();
						switch (formType.getVal(Tc.FORM_CATEGORY).toString()) {
						case Tc.COMPLEX_QUESTIONNAIRE:
							rdr.prepareUiSchemaForComplexQuestionnaire(jsonUISchema, dbo, svr);
							break;
						case Tc.SIMPLE_QUESTIONNAIRE:
							jsonUISchema.addProperty("ui:widget", "radio");
							break;
						default:
							break;
						}
						jsonUISchema.add("ui:options", jsonOptions);
						String fullLabelCodeWithPrefix = dbo.getVal(Tc.LABEL_CODE).toString();
						String labelCode = fullLabelCodeWithPrefix.substring(18, fullLabelCodeWithPrefix.length());
						jObjResult.add(labelCode, jsonUISchema);
					}
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jObjResult.toString()).build();
	}

	@Path("/createNewQuestionnaireAndQuiestions/{sessionId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response createNewQuestionnaireAndQuiestions(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToCreateQuestionnaire";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		Reader rdr = null;
		DbDataObject dboSvFormType = null;
		DbDataObject dboSvFormFieldType = null;
		DbDataArray dbArrLinksToSave = null;
		DbDataArray dbArrQuestions = null;
		DbDataArray dbArrLabelsForQuesitons = null;
		String formCategory = Tc.EMPTY_STRING;
		String fieldType = Tc.EMPTY_STRING;
		String questionnaireTitle = Tc.EMPTY_STRING;
		String question = Tc.EMPTY_STRING;
		Boolean multiEntryBoolean = null;
		Boolean isMandatoryBoolean = null;
		Boolean isNullBoolean = null;
		Boolean isUniqueBoolean = null;
		String localeId = Tc.EMPTY_STRING;
		String objectType = null;
		Boolean autoinstanceSingle = null;
		DbDataObject dboPossibleAnswer = null;
		DbDataArray dbArrLabelsForAnswersToSave = null;
		DbDataArray dbArrPossibleAnswers = null;

		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			rdr = new Reader();
			dbArrLinksToSave = new DbDataArray();
			dbArrQuestions = new DbDataArray();
			dbArrLabelsForQuesitons = new DbDataArray();
			dbArrLabelsForAnswersToSave = new DbDataArray();
			dbArrPossibleAnswers = new DbDataArray();

			// KEY CHECKS
			if (formVals.get(Tc.FORM_CATEGORY) != null && !formVals.get(Tc.FORM_CATEGORY).isEmpty()) {
				formCategory = formVals.get(Tc.FORM_CATEGORY).get(0);
			}
			if (formVals.get(Tc.FIELD_TYPE) != null && !formVals.get(Tc.FIELD_TYPE).isEmpty()) {
				fieldType = formVals.get(Tc.FIELD_TYPE).get(0);
			}
			if (formVals.get(Tc.QUESTIONNAIRE_TITLE) != null && !formVals.get(Tc.QUESTIONNAIRE_TITLE).isEmpty()) {
				questionnaireTitle = formVals.get(Tc.QUESTIONNAIRE_TITLE).get(0);
			}
			if (formVals.get(Tc.QUESTION) != null && !formVals.get(Tc.QUESTION).isEmpty()) {
				question = formVals.get(Tc.QUESTION).get(0);
			}
			if (formVals.get(Tc.MULTI_ENTRY) != null && !formVals.get(Tc.MULTI_ENTRY).isEmpty()) {
				multiEntryBoolean = Boolean.valueOf(formVals.get(Tc.MULTI_ENTRY).get(0));
			}
			if (formVals.get(Tc.IS_MANDATORY) != null && !formVals.get(Tc.IS_MANDATORY).isEmpty()) {
				isMandatoryBoolean = Boolean.valueOf(formVals.get(Tc.IS_MANDATORY).get(0));
			}
			if (formVals.get(Tc.IS_NULL) != null && !formVals.get(Tc.IS_NULL).isEmpty()) {
				isNullBoolean = Boolean.valueOf(formVals.get(Tc.IS_NULL).get(0));
			}
			if (formVals.get(Tc.IS_UNIQUE) != null && !formVals.get(Tc.IS_UNIQUE).isEmpty()) {
				isUniqueBoolean = Boolean.valueOf(formVals.get(Tc.IS_UNIQUE).get(0));
			}
			if (formVals.get(Tc.LOCALE_ID) != null && !formVals.get(Tc.LOCALE_ID).isEmpty()) {
				localeId = formVals.get(Tc.LOCALE_ID).get(0);
			}
			if (formVals.get(Tc.OBJECT_TYPE) != null && !formVals.get(Tc.OBJECT_TYPE).isEmpty()) {
				objectType = formVals.get(Tc.OBJECT_TYPE).get(0);
			}
			if (formVals.get(Tc.AUTOINSTANCE_SINGLE) != null && !formVals.get(Tc.AUTOINSTANCE_SINGLE).isEmpty()) {
				autoinstanceSingle = Boolean.valueOf(formVals.get(Tc.AUTOINSTANCE_SINGLE).get(0));
			}

			// MANDATORY FIELDS CHECKS
			if (formCategory == null || formCategory.trim().equals(Tc.EMPTY_STRING) || fieldType == null
					|| fieldType.trim().equals(Tc.EMPTY_STRING) || questionnaireTitle == null
					|| questionnaireTitle.trim().equals(Tc.EMPTY_STRING) || question == null
					|| question.trim().equals(Tc.EMPTY_STRING) || localeId == null
					|| localeId.trim().equals(Tc.EMPTY_STRING) || multiEntryBoolean == null
					|| isMandatoryBoolean == null || isNullBoolean == null || isUniqueBoolean == null
					|| objectType.trim().isEmpty() || objectType == null || autoinstanceSingle == null) {
				throw new SvException("naits.error.mandatoryFieldsAreMissing", svw.getInstanceUser());
			}
			Long labelCodeParentId = rdr.getSvLocaleObjid(localeId, svr);
			// CREATE QUESTIONNAIRE
			dboSvFormType = wr.createCustomNaitsSvFormType(objectType, formCategory, null, multiEntryBoolean,
					isMandatoryBoolean, autoinstanceSingle, false, svr);
			svw.saveObject(dboSvFormType);
			if (dboSvFormType != null) {
				// CREATE LABEL FOR QUESTIONNAIRE
				DbDataObject dboLabelCodeForQuestionnaire = wr.createLabelForQuestionnaireAndQuestion(
						dboSvFormType.getVal(Tc.LABEL_CODE).toString(), questionnaireTitle, localeId,
						labelCodeParentId);
				svw.saveObject(dboLabelCodeForQuestionnaire);
				for (String tempQuestion : question.split(Tc.QUESTIONS_SEPARATOR)) {
					// CREATE QUESTION
					dboSvFormFieldType = wr.createSvFormFieldTypeObject(fieldType, null, isNullBoolean, isUniqueBoolean,
							formCategory, dboSvFormType.getVal(Tc.LABEL_CODE).toString(), dboSvFormType.getObject_id(),
							false, rdr, svr);
					dbArrQuestions.addDataItem(dboSvFormFieldType);
					// CREATE LABEL FOR QUESTION
					DbDataObject dboLabelCodeForQuestion = wr.createLabelForQuestionnaireAndQuestion(
							dboSvFormFieldType.getVal(Tc.LABEL_CODE).toString(), tempQuestion, localeId,
							labelCodeParentId);
					dbArrLabelsForQuesitons.addDataItem(dboLabelCodeForQuestion);
				}
				if (dbArrQuestions != null && !dbArrQuestions.getItems().isEmpty() && dbArrLabelsForQuesitons != null
						&& !dbArrLabelsForQuesitons.getItems().isEmpty()) {
					svw.saveObject(dbArrQuestions);
					svw.saveObject(dbArrLabelsForQuesitons);
					for (DbDataObject dboTempLabel : dbArrLabelsForQuesitons.getItems()) {
						// force label cache
						I18n.invalidateLabelsCache(dboTempLabel);
					}
				}

				dbArrLinksToSave = wr.createLinkBetweenQuestionAndQuestionnaire(dboSvFormType, dbArrQuestions);
				if (dbArrLinksToSave != null && !dbArrLinksToSave.getItems().isEmpty()) {
					svw.saveObject(dbArrLinksToSave);
				}

				// MANDATORY QUESTIONS
				for (DbDataObject dboLabelCodeForQuestion : dbArrLabelsForQuesitons.getItems()) {
					if (formCategory.equals("2") || formCategory.equals("1")) {
						for (Entry<String, List<String>> entry : formVals.entrySet()) {
							String key = entry.getKey();
							String value = entry.getValue().get(0);
							if (key.equalsIgnoreCase(
									Tc.IS_MANDATORY + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
										dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
								if (dboQuestion != null) {
									wr.createSvParamForMandatoryQuestion(dboQuestion, value, svr);
								}
							}
						}
					}
				}
				for (DbDataObject dboLabelCodeForQuestion : dbArrLabelsForQuesitons.getItems()) {
					if (formCategory.equals("1") && multiEntryBoolean) {
						for (Entry<String, List<String>> entry : formVals.entrySet()) {
							String key = entry.getKey();
							String value = entry.getValue().get(0);
							if (key.equalsIgnoreCase(
									Tc.ADDITIONAL_INFO + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								if (value.equalsIgnoreCase(Tc.DEFINE_ANSWERS)) {
									// 1. Define questions
									for (Entry<String, List<String>> entry1 : formVals.entrySet()) {
										String key1 = entry1.getKey();
										String value1 = entry1.getValue().get(0);
										if (key1.equalsIgnoreCase(
												dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT).toString())) {
											DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
													dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
											DbDataObject parentCode = wr.createParentSvCode(key1, localeId, svw);
											if (parentCode != null) {
												for (String tempPossibleAnswer : value1.split(Tc.QUESTIONS_SEPARATOR)) {
													String generatedSequenceForAnswer = wr
															.generateQuestionnaireAndQuestionSequence(Tc.ANSWER, null,
																	key1, svr);
													// LABELS FOR ANSWER
													DbDataObject dboLabelForAnswer = wr.createLabelForAnswerOption(
															dboSvFormType, dboQuestion, generatedSequenceForAnswer,
															tempPossibleAnswer, localeId, labelCodeParentId);
													dbArrLabelsForAnswersToSave.addDataItem(dboLabelForAnswer);
													// CODES FOR ANSWERS
													dboPossibleAnswer = wr.createChildSvCode(parentCode.getObject_id(),
															key1, dboQuestion.getVal(Tc.LABEL_CODE).toString(),
															localeId, generatedSequenceForAnswer, tempPossibleAnswer,
															false);
													dbArrPossibleAnswers.addDataItem(dboPossibleAnswer);
												}
											}
										}
									}
									if (dbArrLabelsForAnswersToSave != null
											&& !dbArrLabelsForAnswersToSave.getItems().isEmpty()
											&& dbArrPossibleAnswers != null
											&& !dbArrPossibleAnswers.getItems().isEmpty()) {
										svw.saveObject(dbArrLabelsForAnswersToSave);
										svw.saveObject(dbArrPossibleAnswers);
									}
								}
							}
							if (key.equalsIgnoreCase(
									Tc.NUM_ANSWERS + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
										dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
								if (dboQuestion != null) {
									wr.createParamSvFormFieldType(dboQuestion, value, svr);
								}
							}
							if (key.equalsIgnoreCase(
									Tc.NO_ANSWER_OPT + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
										dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
								if (dboQuestion != null) {
									wr.createParamSvFormFieldTypeForNoAnswerOptions(dboQuestion, value, svr);
									if (value.equalsIgnoreCase(Tc.SHORT)) {
										dboQuestion.setVal(Tc.FIELD_TYPE, Tc.NVARCHAR);
										dboQuestion.setVal(Tc.FIELD_SIZE, 200L);
									}
									if (value.equalsIgnoreCase(Tc.LONG)) {
										dboQuestion.setVal(Tc.FIELD_TYPE, Tc.TEXT);
										dboQuestion.setVal(Tc.FIELD_SIZE, 2000L);
									}
									svw.saveObject(dboQuestion);
								}
							}
						}
					}
				}
				// CREATE QUESTION POINTS
				DbDataObject fftScore = null;
				Long score = 0L;
				String correctAnswer = Tc.EMPTY_STRING;
				for (DbDataObject dboLabelCodeForQuestion : dbArrLabelsForQuesitons.getItems()) {
					if (formCategory.equals(Tc.SIMPLE_QUESTIONNAIRE)) {
						for (Entry<String, List<String>> entry : formVals.entrySet()) {
							String key = entry.getKey();
							String value = entry.getValue().get(0);
							if (key.equalsIgnoreCase(Tc.SCORE + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								score = Long.valueOf(value);
							}
							if (key.equalsIgnoreCase(
									Tc.CORRECT_ANSWER + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
								correctAnswer = value;
							}
							if (score != 0L && !correctAnswer.equals(Tc.EMPTY_STRING)) {
								DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
										dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
								if (dboQuestion != null) {
									fftScore = wr.createFftScore(dboSvFormType.getObject_id(),
											dboQuestion.getObject_id(), score, score, Tc.NUMERIC_YES_NO_WITHOUT_CHOOSE,
											correctAnswer);
									svw.saveObject(fftScore);
									score = 0L;
									correctAnswer = Tc.EMPTY_STRING;
									break;
								}
							}
						}
					}
					String typeOfQuestionnaire = Tc.EMPTY_STRING;
					if (formCategory.equals("1") && multiEntryBoolean) {
						DbDataArray dbArrSvParam = null;
						DbDataArray dbArrSvParamValues = null;
						DbDataObject dboSvParamValue = null;
						DbDataObject dboQuestion = rdr.getQuestionObjectByLabelCode(
								dboLabelCodeForQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
						if (dboQuestion != null) {
							dbArrSvParam = svr.getObjectsByParentId(dboQuestion.getObject_id(),
									svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
							if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
								for (DbDataObject tempSvParam : dbArrSvParam.getItems()) {
									dbArrSvParamValues = svr.getObjectsByParentId(tempSvParam.getObject_id(),
											svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
									if (dbArrSvParamValues != null && !dbArrSvParamValues.getItems().isEmpty()) {
										dboSvParamValue = dbArrSvParamValues.get(0);
										if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
											typeOfQuestionnaire = Tc.checkboxes;
										}
										if (dboSvParamValue.getVal(Tc.VALUE).toString().equalsIgnoreCase("short")
												|| dboSvParamValue.getVal(Tc.VALUE).toString()
														.equalsIgnoreCase("long")) {
											typeOfQuestionnaire = Tc.shortOrLong;
										}
										if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
											typeOfQuestionnaire = Tc.radio;
										}
									}
								}
							}
						}
						for (Entry<String, List<String>> entry : formVals.entrySet()) {
							String key = entry.getKey();
							String value = entry.getValue().get(0);
							Long svCodeParent = rdr
									.getCodeList(dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT).toString(), svr);
							DbDataObject dboSvCodeParent = null;
							if (svCodeParent != null) {
								dboSvCodeParent = svr.getObjectById(svCodeParent,
										SvReader.getTypeIdByName(Tc.SVAROG_CODES), null);
							}
							if (typeOfQuestionnaire.equalsIgnoreCase(Tc.checkboxes)) {
								if (key.startsWith(Tc.OPT_SCORE)) {
									DbDataArray questionOptions = rdr
											.getAllQuestionOptionsByQuestionTranslatedLabelCode(
													dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT).toString(), svr);
									if (questionOptions != null && !questionOptions.getItems().isEmpty()) {
										for (DbDataObject tempOption : questionOptions.getItems()) {
											if (key.equalsIgnoreCase(Tc.OPT_SCORE + " "
													+ dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT).toString() + " "
													+ tempOption.getVal(Tc.CODE_VALUE))) {
												Long optScore = Long.valueOf(value);
												fftScore = wr.createFftScore(dboSvFormType.getObject_id(),
														dboQuestion.getObject_id(), 0L, optScore,
														dboSvCodeParent.getVal(Tc.CODE_VALUE).toString(),
														tempOption.getVal(Tc.CODE_VALUE).toString());
												svw.saveObject(fftScore);
											}
										}
									}
								}
							}
							if (typeOfQuestionnaire.equalsIgnoreCase(Tc.radio)) {
								if (key.equalsIgnoreCase(
										Tc.SCORE + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
									score = Long.valueOf(value);
								}
								if (key.equalsIgnoreCase(
										Tc.CORRECT_ANSWER + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
									correctAnswer = value;
								}
								if (score != 0L && !correctAnswer.equals(Tc.EMPTY_STRING)) {
									fftScore = wr.createFftScore(dboSvFormType.getObject_id(),
											dboQuestion.getObject_id(), score, score,
											dboSvCodeParent.getVal(Tc.CODE_VALUE).toString(), correctAnswer);
									svw.saveObject(fftScore);
									score = 0L;
									correctAnswer = Tc.EMPTY_STRING;
									break;
								}
							}
							if (typeOfQuestionnaire.equalsIgnoreCase(Tc.shortOrLong)) {
								if (key.equalsIgnoreCase(
										Tc.SCORE + " " + dboLabelCodeForQuestion.getVal(Tc.LABEL_TEXT))) {
									score = Long.valueOf(value);
								}
								if (score != 0L) {
									fftScore = wr.createFftScore(dboSvFormType.getObject_id(),
											dboQuestion.getObject_id(), score, score, null, null);
									svw.saveObject(fftScore);
									score = 0L;
									correctAnswer = Tc.EMPTY_STRING;
									break;
								}
							}
						}
					}
				}
				result = "naits.success.successfullyCreatedQuestionnaire";
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
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
	 * Web service that return questionnaires data in the questionnaires preview
	 * part
	 * 
	 * @param sessionId
	 * @return
	 */
	@Path("/getQuestionnairesData/{sessionId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response getQuestionnairesData(@PathParam("sessionId") String sessionId) {
		String result = "naits.error.failedToGetQuestionnairesData";
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArrSvFormTypes = null;
		JsonArray jsonArray = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			jsonArray = new JsonArray();
			dbArrSvFormTypes = rdr.getSvFormTypes(svr);
			ArrayList<DbDataObject> items = dbArrSvFormTypes.getSortedItems(Tc.PKID, true);
			dbArrSvFormTypes = new DbDataArray();
			for (int i = items.size(); --i >= 0;) {
				dbArrSvFormTypes.addDataItem(items.get(i));
			}
			if (dbArrSvFormTypes != null && !dbArrSvFormTypes.getItems().isEmpty()) {
				for (DbDataObject dboTempSvFormType : dbArrSvFormTypes.getItems()) {
					rdr.prepareJsonArrayForQuestionnaireData(jsonArray, dboTempSvFormType, svr);
				}
				result = jsonArray.toString();
			}
		} catch (Exception e) {
			log4j.error("General error in processing getQuestionnairesData:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}

	@Path("/getQuestionsAndAnswersFormData/{sessionId}/{questionnaireObjId}/{parentId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuestionsAndAnswersFormData(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireObjId") Long questionnaireObjId, @PathParam("parentId") Long parentId) {
		JSONObject jsonObject = null;
		SvReader svr = null;
		Reader rdr = null;
		DbDataObject dboTempAnswer = null;
		List<Integer> list = null;
		String typeOfQuestionnaire = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			jsonObject = new JSONObject();
			DbDataObject dboQuestionnaire = svr.getObjectById(questionnaireObjId, svCONST.OBJECT_TYPE_FORM_TYPE, null);
			if (dboQuestionnaire != null) {
				DbDataObject dboSvForm = rdr.getSvFormBySvFormType(dboQuestionnaire.getObject_id(), parentId, svr);
				DbDataArray dbArrQuestionsInQuestionnaire = rdr
						.getDbArrayOfSvFormFieldTypeLinkedToSvFormType(questionnaireObjId, svr);
				if (dbArrQuestionsInQuestionnaire != null && !dbArrQuestionsInQuestionnaire.getItems().isEmpty()
						&& dboSvForm != null) {
					for (DbDataObject dboTempQuestion : dbArrQuestionsInQuestionnaire.getItems()) {
						list = new ArrayList<>();
						DbDataArray dbArrTempAnswers = rdr.getAnswerToQuestion(dboTempQuestion.getObject_id(),
								dboSvForm.getObject_id(), svr);
						if (dbArrTempAnswers != null && !dbArrTempAnswers.getItems().isEmpty()) {
							if (dbArrTempAnswers.size() == 1) {
								dboTempAnswer = dbArrTempAnswers.get(0);
							}
							if (dbArrTempAnswers.size() > 1) {
								dbArrTempAnswers.getSortedItems(Tc.PKID, true);
								dboTempAnswer = dbArrTempAnswers.get(dbArrTempAnswers.size() - 1);
							}
							if (dboTempAnswer.getVal(Tc.FIELD_TYPE_ID).equals(dboTempQuestion.getObject_id())
									&& dboTempAnswer.getVal(Tc.FORM_OBJECT_ID).equals(dboSvForm.getObject_id())) {
								String fullLabelCode = dboTempQuestion.getVal(Tc.LABEL_CODE).toString();
								String labelCode = fullLabelCode.substring(18, fullLabelCode.length());
								switch (dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString()) {
								case Tc.COMPLEX_QUESTIONNAIRE:
									DbDataArray dbArrSvParam = svr.getObjectsByParentId(dboTempQuestion.getObject_id(),
											svCONST.OBJECT_TYPE_PARAM, null, 0, 0);
									if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
										for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
											DbDataArray dbArrSvParamValues = svr.getObjectsByParentId(
													dboSvParam.getObject_id(), svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0,
													0);
											if (dbArrSvParamValues != null
													&& !dbArrSvParamValues.getItems().isEmpty()) {
												DbDataObject dboSvParamValue = dbArrSvParamValues.get(0);
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
													typeOfQuestionnaire = Tc.checkboxes;
												}
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
													typeOfQuestionnaire = Tc.radio;
												}
												if (dboSvParamValue.getVal(Tc.VALUE).toString()
														.equalsIgnoreCase("short")
														|| dboSvParamValue.getVal(Tc.VALUE).toString()
																.equalsIgnoreCase("long")) {
													typeOfQuestionnaire = Tc.shortOrLong;
												}
											}
										}
									}
									if (typeOfQuestionnaire.equalsIgnoreCase(Tc.checkboxes)) {
										String[] answers = dboTempAnswer.getVal(Tc.VALUE).toString().split(",");
										int[] answer = new int[answers.length];
										for (int i = 0; i < answers.length; i++) {
											answer[i] = Integer.parseInt(answers[i]);
											list.add(answer[i]);
										}
										jsonObject.putOnce(labelCode, list);
									} else {
										jsonObject.put(labelCode, dboTempAnswer.getVal(Tc.VALUE).toString());
									}
									break;
								case Tc.SIMPLE_QUESTIONNAIRE:
									jsonObject.put(labelCode,
											Integer.parseInt(dboTempAnswer.getVal(Tc.VALUE).toString()));
									break;
								default:
									break;
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jsonObject.toString()).build();
	}

	/**
	 * Web service for answer a question/s in a questionnaire
	 * 
	 * @param sessionId
	 * @param parentId
	 *            Object ID of the parent (HOLDING / ANIMAL)
	 * @param questionnaireObjId
	 *            Questionnaire object ID
	 * @param formVals
	 * @return
	 */
	@Path("/answerQuestionsInQuestionnaire/{sessionId}/{parentId}/{questionnaireObjId}")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/html;charset=utf-8")
	public Response answerQuestionsInQuestionnaire(@PathParam("sessionId") String sessionId,
			@PathParam("parentId") Long parentId, @PathParam("questionnaireObjId") Long questionnaireObjId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToSaveAnswers";
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		Writer wr = null;
		DbDataObject dboSvForm = null;
		DbDataObject dboTempQuestionnaire = null;
		Long parentTypeId = null;
		DbDataObject dboSvFormField = null;
		String typeOfQuestion = Tc.EMPTY_STRING;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			rdr = new Reader();
			wr = new Writer();
			// GET PARENT TYPE ID
			if (formVals != null && !formVals.isEmpty()) {
				for (Entry<String, List<String>> entry : formVals.entrySet()) {
					String key = entry.getKey();
					String value = entry.getValue().get(0);
					if (key.equalsIgnoreCase("parentTypeId")) {
						switch (value) {
						case Tc.HOLDING:
							parentTypeId = SvReader.getTypeIdByName(Tc.HOLDING);
							break;
						case Tc.ANIMAL:
							parentTypeId = SvReader.getTypeIdByName(Tc.ANIMAL);
							break;
						default:
							break;
						}
					}
				}
			}
			// CREATE QUESTIONNAIRE INSTANCE
			if (parentTypeId != null && parentId != null && questionnaireObjId != null) {
				dboTempQuestionnaire = svr.getObjectById(questionnaireObjId,
						SvReader.getTypeIdByName(Tc.SVAROG_FORM_TYPE), null);
				if (dboTempQuestionnaire != null) {
					dboSvForm = rdr.getSvFormBySvFormType(dboTempQuestionnaire.getObject_id(), parentId, svr);
					if (dboSvForm == null) {
						dboSvForm = wr.createSvForm(parentId, dboTempQuestionnaire.getObject_id());
						svw.saveObject(dboSvForm);
					}
				}
			}
			// ANSWER QUESTIONS
			if (formVals != null && !formVals.isEmpty()) {
				for (Entry<String, List<String>> entry : formVals.entrySet()) {
					String question = entry.getKey();
					Long questionObjId = rdr.getQuestionObjIdByLabelCode(question, svr);
					String answer = entry.getValue().get(0);
					if (questionObjId != null && parentId != null && dboSvForm != null) {
						dboSvFormField = wr.answerQuestion(parentId, dboSvForm.getObject_id(), questionObjId, answer,
								svw);
						result = "naits.success.successfullySavedAnswer";

						DbDataObject dboQuestionnaire = svr.getObjectById(questionnaireObjId,
								SvReader.getTypeIdByName(Tc.SVAROG_FORM_TYPE), null);
						if (dboQuestionnaire != null) {
							if (dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString().equals("2")) {
								DbDataObject ffScore = null;
								DbDataObject dboCorrectAnswer = rdr.getCorrectAnswerOfQuestion(
										dboQuestionnaire.getObject_id(), dboSvFormField.getVal(Tc.VALUE).toString(),
										questionObjId, svr);
								if (dboCorrectAnswer != null) {
									ffScore = wr.createFfScore(dboSvForm.getObject_id(), dboSvFormField.getObject_id(),
											(Long) dboCorrectAnswer.getVal(Tc.SCORE));
									svw.saveObject(ffScore);
								}
							}
							if (dboQuestionnaire.getVal(Tc.FORM_CATEGORY).toString().equals("1")) {
								DbDataArray dbArrSvParam = null;
								DbDataArray dbArrSvParamValues = null;
								DbDataObject dboSvParamValue = null;
								if (questionObjId != null) {
									dbArrSvParam = svr.getObjectsByParentId(questionObjId, svCONST.OBJECT_TYPE_PARAM,
											null, 0, 0);
									if (dbArrSvParam != null && !dbArrSvParam.getItems().isEmpty()) {
										for (DbDataObject dboSvParam : dbArrSvParam.getItems()) {
											dbArrSvParamValues = svr.getObjectsByParentId(dboSvParam.getObject_id(),
													svCONST.OBJECT_TYPE_PARAM_VALUE, null, 0, 0);
											if (dbArrSvParamValues != null
													&& !dbArrSvParamValues.getItems().isEmpty()) {
												dboSvParamValue = dbArrSvParamValues.get(0);
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("2")) {
													typeOfQuestion = Tc.checkboxes;
												}
												if (dboSvParamValue.getVal(Tc.VALUE).toString()
														.equalsIgnoreCase("short")
														|| dboSvParamValue.getVal(Tc.VALUE).toString()
																.equalsIgnoreCase("long")) {
													typeOfQuestion = Tc.shortOrLong;
												}
												if (dboSvParamValue.getVal(Tc.VALUE).toString().equals("1")) {
													typeOfQuestion = Tc.radio;
												}
											}
										}
										if (typeOfQuestion.equalsIgnoreCase(Tc.radio)) {
											DbDataObject ffScore = null;
											DbDataObject dboCorrectAnswer = rdr.getCorrectAnswerOfQuestion(
													dboQuestionnaire.getObject_id(),
													dboSvFormField.getVal(Tc.VALUE).toString(), questionObjId, svr);
											if (dboCorrectAnswer != null) {
												ffScore = wr.createFfScore(dboSvForm.getObject_id(),
														dboSvFormField.getObject_id(),
														(Long) dboCorrectAnswer.getVal(Tc.SCORE));
												svw.saveObject(ffScore);
											}
										}
										if (typeOfQuestion.equalsIgnoreCase(Tc.checkboxes)) {
											for (String temp : dboSvFormField.getVal(Tc.VALUE).toString().split(",")) {
												String optNumber = "opt00" + temp;
												DbDataObject dboQuestion = svr.getObjectById(questionObjId,
														SvReader.getTypeIdByName(Tc.SVAROG_FORM_FIELD_TYPE), null);
												DbDataObject dboQuestionLabel = rdr.getQuestionLabelByQuestion(
														dboQuestion.getVal(Tc.LABEL_CODE).toString(), svr);
												if (dboQuestionLabel != null) {
													DbDataObject dboOption = rdr.getCodeListByLabelCode(
															dboQuestionLabel.getVal(Tc.LABEL_TEXT).toString(),
															optNumber, svr);
													if (dboOption != null) {
														DbDataArray fftScores = rdr.getFftScoreForAnswer(questionObjId,
																dboQuestionnaire.getObject_id(),
																dboOption.getVal(Tc.CODE_VALUE).toString(), svr);
														if (fftScores != null && !fftScores.getItems().isEmpty()) {
															for (DbDataObject tempFftScore : fftScores.getItems()) {
																DbDataObject ffScore = wr.createFfScore(
																		dboSvForm.getObject_id(),
																		dboSvFormField.getObject_id(),
																		(Long) tempFftScore.getVal(Tc.SCORE));
																svw.saveObject(ffScore);
															}
														}
													}
												}

											}
										}
										if (typeOfQuestion.equalsIgnoreCase(Tc.shortOrLong)) {
											if (!dboSvFormField.getVal(Tc.VALUE).toString().isEmpty()
													&& dboSvFormField.getVal(Tc.VALUE) != null) {

												DbDataObject dboFftScore = rdr.getFftScorePerAnswer(questionObjId,
														dboQuestionnaire.getObject_id(), svr);
												if (dboFftScore != null) {
													DbDataObject dboFfScore = wr.createFfScore(dboSvForm.getObject_id(),
															dboSvFormField.getObject_id(),
															(Long) dboFftScore.getVal(Tc.SCORE));
													svw.saveObject(dboFfScore);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (questionnaireObjId != null && parentId != null) {
				if (typeOfQuestion.equals(Tc.checkboxes)) {
					Long totalScore = rdr.calculateTotalScoreForMultipleTypeOfQuestionnaire(questionnaireObjId, svr);
					Long achievedScore = rdr.calculateTotalAchievedScoreForMultipleTypeQuestionnaire(questionnaireObjId,
							parentId, svr);
					wr.createSvParamForTotalScore(dboSvForm, achievedScore + Tc.PATH_DELIMITER + totalScore, svr);

				} else {
					Long totalScore = rdr.calculateTotalScoreOfQuestionnaire(questionnaireObjId, svr);
					Long achievedScore = rdr.calculateTotalAchievedScoreOfQuestionnaire(questionnaireObjId, parentId,
							svr);
					wr.createSvParamForTotalScore(dboSvForm, achievedScore + Tc.PATH_DELIMITER + totalScore, svr);
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
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
	 * Web service that export questionnaire via file
	 * 
	 * @param sessionId
	 * @param questionnaireObjId
	 *            Questionnaire object ID
	 * @return
	 */
	@Path("/exportQuestionnaire/{sessionId}/{questionnaireObjId}")
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response exportQuestionnaire(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireObjId") Long questionnaireObjId) {
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				Reader rdr = null;
				try {
					svr = new SvReader(sessionId);
					rdr = new Reader();
					rdr.exportQuestionnaire(questionnaireObjId, output, svr);
				} catch (Exception e) {
					log4j.error("Error printing csv method: exportQuestionnaire", e);
				} finally {
					if (svr != null)
						svr.release();
					output.close();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = " + "questionnaire_labels.csv").build();
	}
	
	/**
	 * Web service that import questionnaire via file
	 * 
	 * @param sessionId
	 * @param file
	 * @return
	 * @throws IOException
	 */
	@Path("/importQuestionnaire/{sessionId}")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Encoded
	@Produces("text/html;charset=utf-8")
	public Response importQuestionnaire(@PathParam("sessionId") String sessionId,
			@FormDataParam("file") InputStream file) throws IOException {
		String result = "naits.error.failedToCreateQuestionnaire";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = new Writer();
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			if (wr.createQuestionnaireViaFile(file, svw, svr)) {
				svw.dbCommit();
				result = "naits.success.successfullyCreatedQuestionnaire";
			}
		} catch (Exception e) {
			log4j.error("Error printing csv method: importQuestionnaire", e);
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
	 * Web service that return questionnaires with scores in HOLDING/ANIMAL
	 * preview data
	 * 
	 * @param sessionId
	 * @param objectType
	 *            Parent type (HOLDING / ANIMAL)
	 * @param objectId
	 *            Parent object ID
	 * @return
	 */
	@Path("/getQuestionnairesByParentTypeId/{sessionId}/{objectType}/{objectId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getQuestionnairesByParentTypeId(@PathParam("sessionId") String sessionId,
			@PathParam("objectType") Long objectType, @PathParam("objectId") Long objectId) {
		String result = "naits.error.failedToGetQuestionnairesData";
		SvReader svr = null;
		Reader rdr = null;
		DbDataArray dbArrSvFormTypes = null;
		JsonArray jsonArray = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			jsonArray = new JsonArray();
			dbArrSvFormTypes = rdr.getDbArrayOfSvFormTypeByParentTypeId(objectType, svr);
			ArrayList<DbDataObject> items = dbArrSvFormTypes.getSortedItems(Tc.PKID, true);
			dbArrSvFormTypes = new DbDataArray();
			for (int i = items.size(); --i >= 0;) {
				dbArrSvFormTypes.addDataItem(items.get(i));
			}
			if (dbArrSvFormTypes != null && !dbArrSvFormTypes.getItems().isEmpty()) {
				for (DbDataObject dboTempSvFormType : dbArrSvFormTypes.getItems()) {
					rdr.prepareJsonArrayForQuestionnairesByParentId(jsonArray, dboTempSvFormType, objectId, svr);
				}
				result = jsonArray.toString();
			}
		} catch (Exception e) {
			log4j.error("General error in processing getQuestionnairesByParentTypeId:", e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(result).build();
	}
	
	@Path("/getAllQuestionnaireAnswers/{sessionId}/{questionnaireObjId}/{parentId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllQuestionnaireAnswers(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireObjId") Long questionnaireObjId, @PathParam("parentId") Long parentId) {
		SvReader svr = null;
		Reader rdr = null;
		Writer wr = null;
		JsonArray jArr = null;
		JsonObject json = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			wr = new Writer();
			jArr = new JsonArray();
			json = new JsonObject();
			DbDataObject svForm = rdr.getSvFormBySvFormType(questionnaireObjId, parentId, svr);
			DbDataArray questionsInQuestionnaire = rdr.getQuestionsInAQuestionnaire(
					svr.getObjectById(questionnaireObjId, svCONST.OBJECT_TYPE_FORM_TYPE, null), svr);
			if (svForm != null && questionsInQuestionnaire != null && !questionsInQuestionnaire.getItems().isEmpty()) {
				for (DbDataObject dboTempQuestion : questionsInQuestionnaire.getItems()) {
					json = wr.createCustomJson(svForm, dboTempQuestion, svr);
					jArr.add(json);
				}
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jArr.toString()).build();
	}
	
	@Path("/deleteQuestionnaire/{sessionId}/{questionnaireObjId}")
	@GET
	@Produces("text/html;charset=utf-8")
	public Response deleteQuestionnaire(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireObjId") Long questionnaireObjId) {
		String result = "naits.error.deleteQuestionnaireObject";
		SvReader svr = null;
		SvWriter svw = null;
		Writer wr = null;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			wr = new Writer();
			svw.dbSetAutoCommit(false);
			Boolean isDeleted = wr.deleteQuestionnaire(questionnaireObjId, svw, svr);
			if (isDeleted) {
				svw.dbCommit();
				result = "naits.success.sucessfullyDeletedQuestionnaireObject";
			}
		} catch (Exception e) {
			if (e instanceof SvException) {
				log4j.error("Error in deleteQuestionnaire: " + ((SvException) e).getFormattedMessage(), e);
			} else
				log4j.error("General error in deleteQuestionnaire", e);
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
	
	@Path("/editQuestionsInQuestionnaire/{sessionId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response editQuestionsInQuestionnaire(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToEditQuestions";
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		Boolean isCommit = false;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			rdr = new Reader();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					String value = entry.getValue().get(0);
					if (key != null && !key.equalsIgnoreCase(Tc.EMPTY_STRING) && !key.equalsIgnoreCase(Tc.SHOULD_DELETE)
							&& value != null && !value.equalsIgnoreCase(Tc.EMPTY_STRING)) {
						DbDataObject questionLabel = rdr.getQuestionLabelByQuestion(key, svr);
						if (questionLabel != null) {
							questionLabel.setVal(Tc.LABEL_TEXT, value);
							svw.saveObject(questionLabel);
							isCommit = true;
						}
					}
				}
			}
			if (isCommit) {
				svw.dbCommit();
				result = "naits.success.successfullyEditedQuestionText";
			}
		} catch (Exception e) {
			log4j.error(e.getMessage());
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
	
	@Path("/getJsonSchemaForEditedQuestionnaires/{sessionId}/{questionnaireLabel}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJsonSchemaForEditedQuestionnaires(@PathParam("sessionId") String sessionId,
			@PathParam("questionnaireLabel") String questionnaireLabel) {
		SvReader svr = null;
		Reader rdr = null;
		JsonObject jObjResult = null;
		try {
			svr = new SvReader(sessionId);
			rdr = new Reader();
			jObjResult = new JsonObject();
			DbDataObject formType = rdr.getSvFormTypeByTitle(questionnaireLabel, svr);
			if (formType != null) {
				DbDataArray dbArrFormFieldTypes = rdr
						.getDbArrayOfSvFormFieldTypeLinkedToSvFormType(formType.getObject_id(), svr);
				jObjResult = rdr.prepareJsonSchemaForEditedQuestionnaire(formType, dbArrFormFieldTypes, svr);
			}
		} catch (Exception e) {
			log4j.error(e.getMessage(), e);
		} finally {
			if (svr != null) {
				svr.release();
			}
		}
		return Response.status(200).entity(jObjResult.toString()).build();
	}
	
	@Path("/deleteQuestionsInQuestionnaire/{sessionId}")
	@POST
	@Produces("text/html;charset=utf-8")
	public Response deleteQuestionsInQuestionnaire(@PathParam("sessionId") String sessionId,
			MultivaluedMap<String, String> formVals) {
		String result = "naits.error.failedToDeleteQuestions";
		SvReader svr = null;
		SvWriter svw = null;
		Reader rdr = null;
		Boolean isCommit = false;
		try {
			svr = new SvReader(sessionId);
			svw = new SvWriter(svr);
			rdr = new Reader();
			for (Entry<String, List<String>> entry : formVals.entrySet()) {
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					String key = entry.getKey();
					String value = entry.getValue().get(0);
					if (key != null && !key.equalsIgnoreCase(Tc.EMPTY_STRING) && value != null
							&& !value.equalsIgnoreCase(Tc.EMPTY_STRING)) {
						DbDataObject question = rdr.getQuestionObjectByLabelCode(key, svr);
						if (question != null && value.equalsIgnoreCase(Tc.SHOULD_DELETE)) {
							DbDataArray questionAnswers = rdr.getQuestionAnswers(question.getObject_id(), svr);
							if (questionAnswers == null || questionAnswers.getItems().isEmpty()) {
								svw.deleteObject(question, false);
								isCommit = true;
							} else {
								result = "naits.error.questionHasBeenAnsweredAndCannotBeDeleted";
							}
						}
					}
				}
			}
			if (isCommit) {
				svw.dbCommit();
				result = "naits.success.successfullyDeletedQuestions";
			}
		} catch (Exception e) {
			log4j.error(e.getMessage());
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
