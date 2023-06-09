package naits_triglav_plugin;

import java.io.File;
import java.io.OutputStream;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.prtech.svarog.svCONST;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPdfExporterParameter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;

public class GeneratePdf {

	static final Logger log4j = LogManager.getLogger(GeneratePdf.class.getName());

	private GeneratePdf() {
	}

	public static Long executeReport(Properties rbConfig, String reportName, String docType, DateTime refDate,
			OutputStream outputStream, Connection conn) {
		Long errorCode = svCONST.SUCCESS;
		File folder = null;
		HashMap<String, Object> hm = null;

		try {
			String path = rbConfig.getProperty(Tc.printJrxmlPath);
			if (path == null) {
				log4j.error(
						"Reports path is not properly configured. Configure the print.jrxml_path variable in svarog.properties");
				return svCONST.GENERAL_ERROR;
			}
			folder = new File(path);
			File[] listOfJrxmls = folder.listFiles();

			if (listOfJrxmls == null) {
				log4j.error(
						"Reports path is not properly configured. Configure the print.jrxml_path variable in svarog.properties "
								+ "to point to an existing dir on your system where the svarog jrxmls can be found!");
				return svCONST.GENERAL_ERROR;
			}

			// Iterate all .jrxml files in PATH folder
			for (File file : listOfJrxmls) {
				if (file.isFile() && file.getName().endsWith("jrxml")) {
					// recompile only if .jasper doesn't exist, or the .jrxml
					// has
					// been modified or overwritten so we would need to
					// recompile
					// otherwise use the previous compilation
					File jrxml = new File(file.getAbsolutePath());
					String jasperFilePath = file.getAbsolutePath().replace(Tc.JRXML_EXTENSION, Tc.JASPER_EXTENSION);
					File jasper = new File(jasperFilePath);
					if (!jasper.exists() || jrxml.lastModified() > jasper.lastModified()) {
						// Recompiling
						log4j.trace("Recompiling jrxml... " + jrxml.getName());
						JasperCompileManager.compileReportToFile(file.getAbsolutePath(), jasperFilePath);
					}
				}
			}
			log4j.trace("Recompiling done!");
			String jasperFileName = path + Tc.PATH_DELIMITER + reportName + Tc.JASPER_EXTENSION;
			if (conn != null && conn.isClosed()) {
				log4j.error("Database connection is closed!");
			}

			hm = setReportParameters(reportName, rbConfig, path);

			JasperPrint jprint = (JasperPrint) JasperFillManager.fillReport(jasperFileName, hm, conn);

			if (docType.equals(Tc.PDF)) {
				// Generate Jasper print
				generatePdf(reportName, jprint, outputStream);
			}
			if (docType.equals(Tc.EXCEL)) {
				// For excel f.r*
				generateExcel(jprint, outputStream);
			}
			errorCode = svCONST.SUCCESS;
		} catch (Exception e) {
			log4j.error("Generating " + docType + " failed!", e);
			errorCode = svCONST.GENERAL_ERROR;
		}
		return errorCode;
	}

	private static void generatePdf(String reportName, JasperPrint print, OutputStream out) throws JRException {
		JRExporter exporter = null;
		exporter = new JRPdfExporter();
		exporter.setParameter(JRPdfExporterParameter.METADATA_TITLE, reportName + Tc.PDF_EXTENSION);
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, out);
		exporter.exportReport();
	}

	private static void generateExcel(JasperPrint print, OutputStream out) throws JRException {
		JRXlsxExporter exporter = new JRXlsxExporter();
		exporter.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, false);
		exporter.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, false);
		exporter.setParameter(JRXlsExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(JRXlsExporterParameter.OUTPUT_STREAM, out);
		exporter.exportReport();
	}

	private static HashMap<String, Object> setReportParameters(String reportName, Properties rbConfig, String path)
			throws ParseException {

		HashMap<String, Object> hm = new HashMap<>();
		DateFormat formatter = new SimpleDateFormat(Tc.DATE_PATTERN);
		hm.put("path", path + Tc.PATH_DELIMITER);

		String objectId = rbConfig.getProperty("OBJ_ID");
		if (objectId == null && rbConfig.get("OBJ_ID") != null) {
			objectId = rbConfig.get("OBJ_ID").toString();
		}
		if (objectId != null && NumberUtils.isNumber(objectId)) {
			if (reportName.equals("EC_wrapper"))
				hm.put("OBJ_ID", objectId);
			else
				hm.put("OBJ_ID", Long.valueOf(objectId));
		}

		String animalId = rbConfig.getProperty("animalid");
		if (animalId != null) {
			hm.put("animalid", animalId);
		}

		String userName = rbConfig.getProperty(Tc.USER_NAME);
		if (userName != null) {
			hm.put(Tc.USER_NAME, userName);
		}

		String villageCode = rbConfig.getProperty("village_code");
		if (villageCode != null) {
			hm.put("village_code", villageCode);
		}

		String dtFrom = rbConfig.getProperty("date_from");
		if (dtFrom != null) {
			if (!reportName.equalsIgnoreCase("sla_daily")) {
				Date dtFromParsed = formatter.parse(dtFrom);
				hm.put("date_from", dtFromParsed);
			} else {
				hm.put("date_from", dtFrom);
			}
		}

		String dtTo = rbConfig.getProperty("date_to");
		if (dtTo != null) {
			java.util.Date dtToParsed = formatter.parse(dtTo);
			hm.put("date_to", dtToParsed);
		}

		String fromDate = rbConfig.getProperty("fromdate");
		if (fromDate != null) {
			hm.put("fromdate", fromDate);
		}

		String toDate = rbConfig.getProperty("todate");
		if (toDate != null) {
			hm.put("todate", toDate);
		}

		String customDate = rbConfig.getProperty("DATE_DMY");
		if (customDate != null) {
			hm.put("DATE_DMY", customDate);
		}

		String customDate1 = rbConfig.getProperty("DATE_DMY_1");
		if (customDate1 != null) {
			hm.put("DATE_DMY_1", customDate1);
		}

		String customDate2 = rbConfig.getProperty("DATE_DMY_2");
		if (customDate2 != null) {
			hm.put("DATE_DMY_2", customDate2);
		}

		String terrCode = rbConfig.getProperty("terr_code");
		if (terrCode != null) {
			hm.put("terr_code", terrCode);
		}

		String activity = rbConfig.getProperty("ACTIVITY");
		if (activity != null) {
			hm.put("ACTIVITY", activity);
		}

		String monthParam = rbConfig.getProperty("month_param");
		if (monthParam != null) {
			hm.put("month_param", monthParam);
		}

		String campaignCode = rbConfig.getProperty("campaign_code");
		if (campaignCode != null) {
			hm.put("campaign_code", Long.valueOf(campaignCode));
		}

		String linkToPostMortemReport = rbConfig.getProperty("LINK_TO_POSTMORTEM_REPORT");
		if (linkToPostMortemReport != null) {
			hm.put("LINK_TO_POSTMORTEM_REPORT", linkToPostMortemReport);
		}

		String stringifiedListOfParams = rbConfig.getProperty("LIST_OF_OBJ_ID");
		if (stringifiedListOfParams != null) {
			// hm.put("LIST_OF_OBJ_ID", Arrays.asList(stringifiedListOfParams.split(",")));
			hm.put("LIST_OF_OBJ_ID", stringifiedListOfParams);
		}

		String companyName = rbConfig.getProperty(Tc.COMPANY_NAME);
		if (companyName != null) {
			if (companyName.equals("null")) {
				companyName = "";
			}
			hm.put(Tc.COMPANY_NAME, companyName);
		}

		String qrCode = rbConfig.getProperty("QR_CODE");
		if (rbConfig.getProperty("QR_CODE") != null) {
			try {
				hm.put("QR_CODE", MatrixToImageWriter
						.toBufferedImage(new QRCodeWriter().encode(qrCode, BarcodeFormat.QR_CODE, 300, 300)));
			} catch (Exception e) {
				log4j.error("Error occurred while generating QR code..");
			}
		}

		return hm;
	}
}