package naits_triglav_plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.prtech.svarog.SvException;
import com.prtech.svarog.SvLock;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class PublicRegistry {

	static final Logger log4j = LogManager.getLogger(PublicRegistry.class.getName());

	/**
	 * Method that deletes existing output file
	 * 
	 * @param path
	 * @return false if file doesn't exist / true if file exist and it's deleted
	 */
	public Boolean deleteExistingOutFile(String path) {
		boolean result = false;
		File outFile = new File(path);
		if (outFile.exists()) {
			result = outFile.delete();
		}
		return result;
	}

	/**
	 * Method that handles XML files
	 * 
	 * @param path
	 * @return Document
	 * @throws Exception
	 */
	public Document getNormalizedXMLFile(String path) {
		Document document = null;
		File f = new File(path);
		if (f.exists()) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;
			try {
				builder = dbf.newDocumentBuilder();
				document = builder.parse(f);
				document.getDocumentElement().normalize();
			} catch (Exception e) {
				log4j.error("Error while parsing string into xml file! Method: getNormalizedXMLFile, Error message", e);
			}
		}
		return document;
	}

	private Boolean setHoldingResponsibleDbDataObjectAccordingXMLFile(Document outputXMLFile,
			DbDataObject dboHoldingResponsible) {
		Boolean result = false;
		NodeList nodeList = null;
		String firstName = "";
		String lastName = "";
		String fullName = "";
		try {
			nodeList = outputXMLFile.getElementsByTagName(Tc.PUBREG_PERSON);
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
					Element node = (Element) nodeList.item(i);
					NodeList childNodes = node.getChildNodes();
					for (int j = 0; j < childNodes.getLength(); j++) {
						if (childNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
							Element childNode = (Element) childNodes.item(j);
							if (Tc.PUBREG_FIRST_NAME.equals(childNode.getNodeName())) {
								dboHoldingResponsible.setVal(Tc.FIRST_NAME, childNode.getTextContent());
								firstName = childNode.getTextContent();
							}
							if (Tc.PUBREG_LAST_NAME.equals(childNode.getNodeName())) {
								dboHoldingResponsible.setVal(Tc.LAST_NAME, childNode.getTextContent());
								lastName = childNode.getTextContent();
							}
							if (Tc.PUBREG_BIRTH_DATE.equals(childNode.getNodeName())) {
								String birthDate = childNode.getTextContent().substring(0, 10).trim();
								dboHoldingResponsible.setVal(Tc.BIRTH_DATE, birthDate);
							}
							if (Tc.PUBREG_REGISTRATION.equals(childNode.getNodeName())) {
								NodeList regNodeList = node.getElementsByTagName(childNode.getNodeName());
								Element regNode = (Element) regNodeList.item(0);
								NodeList regChildNodes = regNode.getChildNodes();
								for (int p = 0; p < regChildNodes.getLength(); p++) {
									if (childNodes.item(p).getNodeType() == Node.ELEMENT_NODE) {
										Element regChildNode = (Element) regChildNodes.item(p);
										if (Tc.PUBREG_ADDRESS.equals(regChildNode.getNodeName())) {
											dboHoldingResponsible.setVal(Tc.ADDRESS, regChildNode.getTextContent());
										}
									}
								}
							}
						}
					}
				}
			}
			if (dboHoldingResponsible.getVal(Tc.ADDRESS) == null
					|| dboHoldingResponsible.getVal(Tc.ADDRESS).toString().trim().equals(Tc.EMPTY_STRING)) {
				dboHoldingResponsible.setVal(Tc.ADDRESS, Tc.NOT_AVAILABLE_NA);
			}
			dboHoldingResponsible.setVal(Tc.HOLDER_TYPE, "1");
			dboHoldingResponsible.setStatus(Tc.VALID);
			fullName = firstName.trim() + " " + lastName.trim();
			if (!fullName.trim().equals(Tc.EMPTY_STRING)) {
				dboHoldingResponsible.setVal(Tc.FULL_NAME, fullName);
			} else {
				dboHoldingResponsible.setVal(Tc.FULL_NAME, Tc.NOT_AVAILABLE_NA);
			}
		} catch (Exception e) {
			log4j.error(
					"Error while processing file!Method: setHoldingResponsibleDbDataObjectAccordingXMLFile, Error message",
					e);
		}
		return result;
	}

	/**
	 * Custom method
	 * 
	 * @param inputPath
	 *            Path of the XML file
	 * @param paramPrivateNumber
	 *            Private number value
	 * @param paramBirthYear
	 *            Birth date value
	 * @param paramLastName
	 *            Last name value
	 * @return true if we successfully transform the file without errors, false
	 *         if exception is thrown
	 * @throws SvException
	 * 
	 */
	public boolean createOrUpdateExistingInputXMLFile(String inputXMLFilePath, String paramPrivateNumber,
			String paramBirthYear, String paramLastName, SvReader svr) throws SvException {
		boolean result = true;
		ReentrantLock lock = null;
		try {
			lock = SvLock.getLock(inputXMLFilePath, false, 0);
			if (lock == null) {
				throw (new SvException("naits.error.pubregCheckUsedByOtherUsers", svr.getInstanceUser()));
			}
			Document document = getNormalizedXMLFile(inputXMLFilePath);
			try {
				setInputXMLFileWithPersonParams(document, paramPrivateNumber, paramBirthYear, paramLastName);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				DOMSource domSource = new DOMSource(document);
				StreamResult streamResult = new StreamResult(new File(inputXMLFilePath));
				transformer.transform(domSource, streamResult);
			} catch (Exception e) {
				result = false;
				log4j.error("Error while parsing string xml file! Error message", e);
			}
		} finally {
			if (lock != null) {
				SvLock.releaseLock(inputXMLFilePath, lock);
			}
		}
		return result;
	}

	/**
	 * Custom method that sets Person child XML elements
	 * 
	 * @param doc
	 * @param paramPrivateNumber
	 * @param paramBirthYear
	 * @param paramLastName
	 * @throws Exception
	 */
	private void setInputXMLFileWithPersonParams(Document doc, String paramPrivateNumber, String paramBirthYear,
			String paramLastName) throws Exception {
		NodeList nodeList = doc.getElementsByTagName("Person");
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element node = (Element) nodeList.item(i);
				NodeList childNodes = node.getChildNodes();
				for (int j = 0; j < childNodes.getLength(); j++) {
					if (childNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
						Element childNode = (Element) childNodes.item(j);
						if (paramPrivateNumber != null && childNode.getNodeName().equals("PrivateNumber")) {
							childNode.setTextContent(paramPrivateNumber);
						}
						if (paramBirthYear != null && childNode.getNodeName().equals("BirthYear")) {
							childNode.setTextContent(paramBirthYear);
						}
						if (paramLastName != null && childNode.getNodeName().equals("LastName")) {
							childNode.setTextContent(paramLastName);
						}
					}
				}
			}
		}
	}

	public File createTempInputFile(String publicRegistryPath, String inputWrapperPath) throws IOException {
		File tempInputFile = null;
		File inputWrapperFile = null;
		String tempDirPath = publicRegistryPath + "//Temp//";
		DateTime dtNow = new DateTime();
		tempInputFile = new File(tempDirPath + "input_" + dtNow.getMillis() + Tc.XML_EXTENSION);
		if (!tempInputFile.exists()) {
			tempInputFile.createNewFile();
		}
		inputWrapperFile = new File(inputWrapperPath);
		try (FileInputStream fis = new FileInputStream(inputWrapperFile);
				InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(isr);
				FileOutputStream fos = new FileOutputStream(tempInputFile);
				OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);) {
			String data = null;
			while ((data = br.readLine()) != null) {
				bw.write(data);
				bw.newLine();
			}
		} catch (Exception e) {
			log4j.error("Error occurred while creating temporary file... ", e);
		}
		return tempInputFile;
	}

	public Boolean runPublicRegistryCheck(String publicRegistryKeyStoragePath, String publicRegistryMainPath,
			String exePath, String inputPath, String outputPath, String paramPrivateNumber, String paramBirthYear,
			String paramLastName, SvReader svr) throws SvException, IOException {
		Boolean result = false;
		Process process = null;
		File tempInputFile = null;
		String keyStoragePath = publicRegistryKeyStoragePath + Tc.KEY_STORAGE;
		String line = null;
		BufferedReader br = null;
		ArrayList<String> responseMessages = new ArrayList<>();
		try {
			tempInputFile = createTempInputFile(publicRegistryMainPath, inputPath);
			deleteExistingOutFile(outputPath);
			if (createOrUpdateExistingInputXMLFile(tempInputFile.getAbsolutePath(), paramPrivateNumber, paramBirthYear,
					paramLastName, svr)) {
				ProcessBuilder pb = new ProcessBuilder(exePath, tempInputFile.getAbsolutePath(), outputPath,
						keyStoragePath, Boolean.FALSE.toString());
				pb.directory(new File(publicRegistryMainPath));
				process = pb.start();

				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				br = new BufferedReader(isr);

				while ((line = br.readLine()) != null) {
					responseMessages.add(line.trim());
					if (line.startsWith(Tc.DESTROY_PROCESS)) {
						process.destroy();
					}
				}
			}
			if (responseMessages.isEmpty()) {
				log4j.error("Something went wrong! Process didn't start!");
			} else {
				if (responseMessages.get(responseMessages.size() - 3).trim()
						.equals(Tc.CANNOT_CONNECT_TO_REMOTE_SERVER.trim())) {
					ProcessBuilder pb = new ProcessBuilder(exePath, tempInputFile.getAbsolutePath(), outputPath,
							keyStoragePath, Boolean.FALSE.toString());
					pb.directory(new File(publicRegistryMainPath));
					process = pb.start();

					InputStream is = process.getInputStream();
					InputStreamReader isr = new InputStreamReader(is);
					br = new BufferedReader(isr);

					while ((line = br.readLine()) != null) {
						responseMessages.add(line);
						if (line.startsWith(Tc.DESTROY_PROCESS)) {
							process.destroy();
						}
					}
					if (responseMessages.isEmpty()) {
						log4j.error("Something went wrong! Process didn't start!");
					}
				}
			}
			Document outputXMLFile = getNormalizedXMLFile(outputPath);
			if (outputXMLFile != null) {
				result = true;
			}
		} catch (IOException e) {
			log4j.error("Error while processing file! Error message", e);
		} finally {
			try {
				if (process != null) {
					if (process.getInputStream() != null)
						process.getInputStream().close();
					if (process.getOutputStream() != null)
						process.getOutputStream().close();
					if (process.getErrorStream() != null)
						process.getErrorStream().close();
				}
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				log4j.error("Error while closing process streams!\nError message", e);
			}
		}
		return result;
	}

	/**
	 * Method that checks if Holding Responsible is valid
	 * 
	 * @param dboHoldingResponsible
	 * @return true / false
	 * @throws SvException
	 * @throws IOException
	 */
	public Boolean publicRegistryCheck(DbDataObject dboHoldingResponsible, String publicRegistryKeyStoragePath,
			String publicRegistryMainPath, SvReader svr) throws SvException {
		Boolean result = false;
		boolean isOutputXMLFileGenerated = false;
		String privateNumber = null;
		String birthYear = null;
		String lastName = null;
		DateTime dtNow = new DateTime();
		String exePath = publicRegistryMainPath + Tc.PUBREG_EXE;
		String inputXMLFile = publicRegistryMainPath + Tc.BIRTH_YEAR_PR_WRAPPER_XML;
		String outputXMLFile = publicRegistryMainPath + "//Output//PersonResult_" + dboHoldingResponsible.getObject_id()
				+ dtNow.getMillis() + Tc.XML_EXTENSION;
		try {
			if (dboHoldingResponsible.getVal(Tc.NAT_REG_NUMBER) != null) {
				privateNumber = dboHoldingResponsible.getVal(Tc.NAT_REG_NUMBER).toString();
				if (dboHoldingResponsible.getVal(Tc.BIRTH_DATE) != null) {
					DateTime dtBirthDate = new DateTime(dboHoldingResponsible.getVal(Tc.BIRTH_DATE).toString());
					birthYear = String.valueOf(dtBirthDate.getYear()).trim();
					isOutputXMLFileGenerated = runPublicRegistryCheck(publicRegistryKeyStoragePath,
							publicRegistryMainPath, exePath, inputXMLFile, outputXMLFile, privateNumber, birthYear,
							lastName, svr);
					if (isOutputXMLFileGenerated) {
						result = true;
						Document doc = getNormalizedXMLFile(outputXMLFile);
						setHoldingResponsibleDbDataObjectAccordingXMLFile(doc, dboHoldingResponsible);
					} else if (dboHoldingResponsible.getVal(Tc.LAST_NAME) != null) {
						lastName = dboHoldingResponsible.getVal(Tc.LAST_NAME).toString();
						inputXMLFile = publicRegistryMainPath + Tc.LAST_NAME_PR_WRAPPER_XML;
					}
				} else if (dboHoldingResponsible.getVal(Tc.LAST_NAME) != null) {
					lastName = dboHoldingResponsible.getVal(Tc.LAST_NAME).toString();
					inputXMLFile = publicRegistryMainPath + Tc.LAST_NAME_PR_WRAPPER_XML;
				} else {
					return result;
				}
				if (!isOutputXMLFileGenerated) {
					isOutputXMLFileGenerated = runPublicRegistryCheck(publicRegistryKeyStoragePath,
							publicRegistryMainPath, exePath, inputXMLFile, outputXMLFile, privateNumber, birthYear,
							lastName, svr);
					if (isOutputXMLFileGenerated) {
						result = true;
						Document doc = getNormalizedXMLFile(outputXMLFile);
						setHoldingResponsibleDbDataObjectAccordingXMLFile(doc, dboHoldingResponsible);
					}
				}
				if (dboHoldingResponsible.getVal(Tc.IS_PROCESSED) == null) {
					dboHoldingResponsible.setVal(Tc.IS_PROCESSED, true);
				}
			}
		} catch (Exception e) {
			log4j.error(e);
		}
		return result;
	}
}
