package naits_triglav_plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog.svCONST;
import com.prtech.svarog_common.DbDataObject;

@Path("/naits_triglav_plugin/report")
public class Report {
	String jrxmlHolder = "";
	static final Logger log4j = LogManager.getLogger(Report.class.getName());

	@GET
	@Path("/generatePdf/{session_id}/{object_id}/{reportname}")
	@Produces("application/pdf")
	public StreamingOutput generatePdf(@PathParam("session_id") final String sessionId,
			@PathParam("object_id") final String object_id, @PathParam("reportname") final String reportname,
			@Context HttpServletRequest httpRequest) {
		return new StreamingOutput() {
			public void write(OutputStream output) {
				SvReader dbu = null;
				DbDataObject userObj = null;
				DbDataObject exportCert = null;
				String tempObjectId = object_id;
				try {
					try {
						dbu = new SvReader(sessionId);
						if (object_id == null || object_id.equals("null")) {
							tempObjectId = "0";
						}
						userObj = SvReader.getUserBySession(sessionId);
					} catch (Exception e) {
						log4j.error("Cannot instantiate DbUtil when trying to generate report!", e);
						return;
					}
					DateTime ref_date = new DateTime();
					// get
					String printParam = SvConf.getParam("print.jrxml_path");
					Properties rb = new Properties();
					rb.put("print.jrxml_path", printParam);
					switch (reportname) {
					case "StatusUpdatePrintoutWrapper":
						rb.put("village_code", tempObjectId.toString());
						break;
					case "EC_wrapper":
						exportCert = dbu.getObjectById(Long.parseLong(tempObjectId),
								SvReader.getTypeIdByName(Tc.EXPORT_CERT), null);
						if (exportCert != null && userObj != null && userObj.getObject_id() != null
								&& userObj.getVal(Tc.USER_NAME) != null && exportCert != null
								&& exportCert.getVal(Tc.EXP_CERTIFICATE_ID) != null) {
							rb.put("OBJ_ID", exportCert.getVal(Tc.EXP_CERTIFICATE_ID).toString());
							rb.put("USER_ID", userObj.getObject_id());
						}
						break;
					case "ahsm_areas":
						if (userObj != null && userObj.getVal(Tc.USER_NAME) != null) {
							rb.put(Tc.USER_NAME, userObj.getVal(Tc.USER_NAME).toString());
						} else {
							rb.put(Tc.USER_NAME, "N/A");
						}
						break;
					case "inventory_mod_main":
						if (userObj != null && userObj.getVal(Tc.USER_NAME) != null)
							rb.put(Tc.USER_NAME, userObj.getVal(Tc.USER_NAME).toString());
						else
							rb.put(Tc.USER_NAME, "N/A");
						break;
					default:
						break;
					}
					if (rb.get("OBJ_ID") == null && !reportname.equals("ahsm_areas"))
						rb.put("OBJ_ID", tempObjectId.toString());
					/*
					 * if (reportname.equals("StatusUpdatePrintoutWrapper"))
					 * rb.put("village_code", object_id.toString()); else if
					 * (reportname.equals("EC_main")) rb.put("USER_ID",
					 * userObj.getVal(Tc.USER_NAME).toString()); else
					 * rb.put("OBJ_ID", object_id.toString());
					 */
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					IOUtils.write(data, output);
					GeneratePdf.executeReport(rb, reportname, "PDF", ref_date, output, dbu.dbGetConn());
				} catch (Exception e) {
					log4j.error("Error printing PDF!", e);
				} finally {
					if (dbu != null)
						dbu.release();
				}
			}
		};
	}

	@GET
	@Path("/generatePdf/{session_id}/{object_id}/{reportname}/{date_from}/{date_to}")
	@Produces("application/pdf")
	public StreamingOutput generatePdf(@PathParam("session_id") final String sessionId,
			@PathParam("object_id") final String object_id, @PathParam("reportname") final String reportname,
			@PathParam("date_from") final String date_from, @PathParam("date_to") final String date_to,
			@Context HttpServletRequest httpRequest) {
		return new StreamingOutput() {
			public void write(OutputStream output) {
				SvReader dbu = null;
				Reader rdr = null;
				try {
					try {
						dbu = new SvReader(sessionId);
						rdr = new Reader();
					} catch (Exception e) {
						log4j.error("Cannot instantiate DbUtil when trying to generate report!", e);
						return;
					}
					DateTime ref_date = new DateTime();
					// get
					String customDateFrom = rdr.customDateFormatter(date_from);
					String printParam = SvConf.getParam("print.jrxml_path");
					Properties rb = new Properties();
					rb.put("print.jrxml_path", printParam);
					if (reportname.equals("StatusUpdatePrintoutWrapper"))
						rb.put("village_code", object_id);
					else if (reportname.equals("animal_born") || reportname.equals("animal_death")
							|| reportname.equals("animal_moveon") || reportname.equals("animal_moveoff")
							|| reportname.equals("animal_lost")) {
						rb.put("village_code", object_id);
						rb.put("date_from", date_from);
						rb.put("date_to", date_to);
					} else {
						rb.put("OBJ_ID", object_id);
						if (reportname.equalsIgnoreCase("SLA_DAILY")) {
							if (date_from == null || date_from.equals("null")) {
								customDateFrom = rdr.customDateFormatter(ref_date.toString().substring(0, 10));
							}
							rb.put("date_from", customDateFrom);
						}
					}

					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					IOUtils.write(data, output);
					GeneratePdf.executeReport(rb, reportname, "PDF", ref_date, output, dbu.dbGetConn());
				} catch (Exception e) {
					log4j.error("Error printing PDF!", e);
				} finally {
					if (dbu != null)
						dbu.release();
				}
			}
		};
	}

	@GET
	@Path("/generatePdf/{session_id}/{object_id}/{reportname}/{date_from}/{date_to}/{user_name}")
	@Produces("application/pdf")
	public StreamingOutput generatePdf(@PathParam("session_id") final String sessionId,
			@PathParam("object_id") final String object_id, @PathParam("reportname") final String reportname,
			@PathParam("date_from") final String date_from, @PathParam("date_to") final String date_to,
			@PathParam("user_name") final String user_name, @Context HttpServletRequest httpRequest) {
		return new StreamingOutput() {
			public void write(OutputStream output) {
				SvReader svr = null;
				Reader rdr = null;
				try {
					rdr = new Reader();
					try {
						svr = new SvReader(sessionId);
					} catch (Exception e) {
						log4j.error("Cannot instantiate DbUtil when trying to generate report!", e);
						return;
					}
					String userName = user_name;
					if (!reportname.equals("StatusUpdatePrintoutWrapper") && !reportname.equals("vaccEventSummary")) {
						if (user_name == null || user_name.equals("null")) {
							userName = "";
						}
						if (!userName.equals("")
								&& rdr.findAppropriateUserByUserName(user_name.replace("-", "."), svr) == null) {
							log4j.error(
									"User: " + user_name + " not found in NAITS. Invoice report can not be generated.");
							svr.release();
							throw (new SvException("naits.error.invReportNotGenarated.userNotFound", svCONST.systemUser,
									null, null));
						}
					}
					DateTime ref_date = new DateTime();
					String printParam = SvConf.getParam("print.jrxml_path");
					Properties rb = new Properties();
					rb.put("print.jrxml_path", printParam);
					if (reportname.equals("INV_main") || reportname.equals("INV2_main")) {
						String customDateFrom = rdr.customDateFormatter(date_from);
						String customDateTo = rdr.customDateFormatter(date_to);
						rb.put("terr_code", object_id.toString());
						if (!userName.equals("") && userName.contains("-")) {
							userName = user_name.replace("-", ".");
						}
						rb.put(Tc.USER_NAME, userName.toUpperCase());
						rb.put("fromdate", customDateFrom);
						rb.put("todate", customDateTo);
					} else if (reportname.equals("StatusUpdatePrintoutWrapper")) {
						Long campaignObj_Id = Long.valueOf(user_name);
						String activity_Name = "";
						DbDataObject dboVaccinationEvent = svr.getObjectById(campaignObj_Id,
								SvReader.getTypeIdByName(Tc.VACCINATION_EVENT), null);
						if (dboVaccinationEvent != null) {
							activity_Name = rdr.campaignActivityNameInCustomFormat(dboVaccinationEvent, svr);
						}
						rb.put("village_code", object_id.toString());
						rb.put("ACTIVITY", activity_Name);
					} else if (reportname.equals("vaccEventSummary")) {
						rb.put("OBJ_ID", object_id);
						rb.put("terr_code", user_name);
						rb.put("month_param", date_to);
					}
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					IOUtils.write(data, output);
					GeneratePdf.executeReport(rb, reportname, "PDF", ref_date, output, svr.dbGetConn());
				} catch (Exception e) {
					log4j.error("Error printing PDF!", e);
				} finally {
					if (svr != null)
						svr.release();
				}
			}
		};
	}

	/**
	 * Web service for generating PDF/Excel files. No mater that the web service
	 * produces "application/vnd.ms-excel", when we want to print to PDF,
	 * JRPdfExporterParameter has property for overriding the extension of the
	 * file
	 * 
	 * @param sessionId
	 *            Session ID
	 * @param objectId
	 *            Object ID
	 * @param printType
	 *            Print type (PDF/Excel)
	 * @param reportName
	 *            Report name
	 * @return PDF or Excel file
	 * @author daut
	 */
	@GET
	@Path("/generatePdfOrExcel/{sessionId}/{objectId}/{printType}/{customDate}/{customDate2}/{campaignId}/{reportName}")
	@Produces("application/vnd.ms-excel")
	public Response generatePdfOrExcel(@PathParam("sessionId") final String sessionId,
			@PathParam("objectId") final String objectId, @PathParam("printType") final String printType,
			@PathParam("customDate") final String customDate, @PathParam("customDate2") final String customDate2,
			@PathParam("campaignId") final String campaignId, @PathParam("reportName") final String reportName,
			@Context HttpServletRequest httpRequest) {
		String fileExtension = printType.equals(Tc.EXCEL) ? Tc.XLS_EXTENSION : Tc.PDF_EXTENSION;
		StreamingOutput fileStream = new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				SvReader svr = null;
				Properties prop = null;
				Reader rdr = null;
				try {
					svr = new SvReader(sessionId);
					prop = new Properties();
					rdr = new Reader();

					DateTime refDate = new DateTime();

					String printParam = SvConf.getParam("print.jrxml_path");
					prop.put("print.jrxml_path", printParam);

					switch (reportName) {
					case "STAT_ACBV":
					case "STAT_ACBM":
					case "STAT_ASBV":
					case "STAT_FCBV":
					case "STAT_HTBV":
						prop.put("village_code", objectId);
						prop.put("DATE_DMY", customDate);
						break;
					case "STAT_ACBH":
						prop.put("OBJ_ID", objectId);
						prop.put("DATE_DMY", customDate);
						break;
					case "STAT_AEBC":
					case "STAT_IABC":
					case "STAT_SABC":
					case "STAT_SABD":
						prop.put("DATE_DMY_1", customDate);
						prop.put("DATE_DMY_2", customDate2);
						break;
					case "STAT_AEBV":
					case "STAT_TAG_REPLACEMENT":
						prop.put("village_code", objectId);
						prop.put("DATE_DMY_1", customDate);
						prop.put("DATE_DMY_2", customDate2);
						break;
					case "STAT_ECBC":
					case "STAT_FCBC":
					case "STAT_HTBC":
					case "STAT_HTGT":
						prop.put("DATE_DMY", customDate);
						break;
					case "STAT_CMPV":
						prop.put("village_code", objectId);
						prop.put("campaign_code", campaignId);
						prop.put("DATE_DMY_1", customDate);
						prop.put("DATE_DMY_2", customDate2);
						break;
					case "INV_main":
					case "INV2_main":
						String customDateFrom = rdr.customDateFormatter(customDate);
						String customDateTo = rdr.customDateFormatter(customDate2);
						prop.put("terr_code", objectId.toString());
						String userName = campaignId == null || campaignId.equalsIgnoreCase("null") ? "" : campaignId;
						if (!userName.equals("")) {
							if (userName.contains("-")) {
								userName = userName.replace("-", ".");
							}
							if (rdr.findAppropriateUserByUserName(userName, svr) == null) {
								log4j.error("User: " + campaignId
										+ " not found in NAITS. Invoice report can not be generated.");
								svr.release();
								throw (new SvException("naits.error.invReportNotGenarated.userNotFound",
										svCONST.systemUser, null, null));
							}
						}
						prop.put(Tc.USER_NAME, userName.toUpperCase());
						prop.put("fromdate", customDateFrom);
						prop.put("todate", customDateTo);
					default:
						prop.put("OBJ_ID", objectId);
						break;
					}
					ByteArrayOutputStream bstr = new ByteArrayOutputStream();
					byte[] data = bstr.toByteArray();
					output.write(data);
					GeneratePdf.executeReport(prop, reportName, printType, refDate, output, svr.dbGetConn());
				} catch (Exception e) {
					log4j.error("Error printing PDF/Excel! method: generatePdfOrExcel", e);
				} finally {
					if (svr != null)
						svr.release();
				}
			}
		};
		return Response.ok(fileStream, MediaType.APPLICATION_OCTET_STREAM)
				.header("content-disposition", "attachment; filename = " + reportName + fileExtension).build();
	}
}
