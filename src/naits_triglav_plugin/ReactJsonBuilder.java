/*******************************************************************************
 * Copyright (c) 2017, 2018 Tibro DOOEL Skopje.
 * All rights reserved.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See LICENSE file in the project root for the specific language governing 
 * permissions and limitations under the License.
 *
 *******************************************************************************/
package naits_triglav_plugin;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.prtech.svarog.I18n;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

/**
 * Class for managing and creation of custom json dedicated for React
 * environment
 * 
 * @author TIBRO_001
 *
 */

public class ReactJsonBuilder {
	/**
	 * Log4j instance used for logging
	 */
	static final Logger log4j = LogManager.getLogger(ReactJsonBuilder.class.getName());

	/**
	 * Method for generation of JSON UI schema depending on certain custom react
	 * component
	 * 
	 * @param reactComponent
	 * @param svData
	 * @return
	 */
	public String prepareReactJsonUISchema(String reactComponent, DbDataArray svData) {
		String finalResult = "";
		switch (reactComponent) {
		case "DROPDOWN":
			finalResult = prepareHeaderJson(reactComponent) + prepareWindowTitle(reactComponent)
					+ prepareWindowYNDoc(reactComponent, svData);
			break;
		default:
			break;
		}
		finalResult = matchBrackets(finalResult);
		return finalResult;
	}

	/**
	 * Method for generation of JSON form data depending on certain custom react
	 * component
	 * 
	 * @param reactComponent
	 * @param svData
	 * @return
	 */
	public String prepareReactJsonFormData(String reactComponent, DbDataArray svData) {
		String finalResult = "";
		switch (reactComponent) {
		case "DROPDOWN":
			finalResult = prepareHeaderJson(reactComponent) + prepareYNDocData(reactComponent, svData);
			break;
		default:
			break;
		}
		finalResult = matchBrackets(finalResult);
		return finalResult;
	}

	/**
	 * Method for generation of JSON form data depending on certain custom react
	 * component
	 * 
	 * @param reactComponent
	 * @param svData
	 * @return
	 */
	public String prepareReactJson(String reactComponent, DbDataArray svData) {
		String finalResult = "";
		switch (reactComponent) {
		case "DROPDOWN":
			finalResult = prepareHeaderJson(reactComponent) + prepareWindowTitle(reactComponent);
			break;
		default:
			break;
		}
		finalResult = matchBrackets(finalResult);
		return finalResult;
	}

	public String prepareHeaderJson(String reactComponent) {
		String result = "";
		switch (reactComponent) {
		case "DROPDOWN":
			String resultType = "SUCCESS";
			String titleLabel = "success.preJsonString";
			String messageLabel = "success.preJsonString";
			result = "{\"type\":" + "\"" + resultType + "\"" + ",\"title\":" + "\"" + titleLabel + "\""
					+ ",\"message\":" + "\"" + messageLabel + "\"" + ",\"data\": {";
			break;
		default:
			break;
		}
		return result;
	}

	public String prepareWindowTitle(String reactComponent) {
		String result = "";
		switch (reactComponent) {
		case "DROPDOWN":
			String windowTitle = "windowTitle.label";
			String windowsType = "object";
			result = "\"title\":" + "\"" + windowTitle + "\"" + ",\"type\":" + "\"" + windowsType + "\""
					+ ",\"properties\": {";
		}

		return result;
	}

	public String prepareWindowYNDoc(String reactComponent, DbDataArray svDataArray) {
		String finalResult = "";
		switch (reactComponent) {
		case "DROPDOWN":
			for (DbDataObject temp : svDataArray.getItems()) {
				String result = "";
				if (temp.getVal(Tc.LABEL_CODE) != null) {
					result = "\"" + temp.getVal(Tc.LABEL_CODE).toString() + "\":{" + "\"type\": \"number\",\"title\":"
							+ "\"" + I18n.getText(temp.getVal(Tc.LABEL_CODE).toString())
							+ "\",\"enum\":[0,1],\"enumNames\":[\"Не\",\"Да\"]},";
					finalResult += result;
				}
			}
			break;
		default:
			break;
		}
		if (finalResult.length() > 1) {
			finalResult = finalResult.substring(0, finalResult.length() - 1);
		}
		return finalResult;
	}

	public String prepareYNDocData(String reactComponent, DbDataArray svDataArray) {
		String finalResult = "";
		switch (reactComponent) {
		case "DROPDOWN":
			for (DbDataObject temp : svDataArray.getItems()) {
				String result = "";
				if (temp.getVal(Tc.LABEL_CODE) != null && temp.getVal(Tc.VALUE) != null) {
					result = "\"" + temp.getVal(Tc.LABEL_CODE).toString() + "\":" + temp.getVal(Tc.VALUE).toString()
							+ ",";
					finalResult += result;
				}
			}
			break;
		default:
			break;
		}
		if (finalResult.length() > 1) {
			finalResult = finalResult.substring(0, finalResult.length() - 1);
		}
		return finalResult;
	}

	public String matchBrackets(String buildedJson) {
		String result = "";
		if (buildedJson.length() > 1) {
			int cnt_openBrackets = StringUtils.countMatches(buildedJson, "{");
			int cnt_closedBrackets = StringUtils.countMatches(buildedJson, "}");
			if (cnt_openBrackets > cnt_closedBrackets) {
				int diff = cnt_openBrackets - cnt_closedBrackets;
				for (int i = 0; i < diff; i++) {
					buildedJson = buildedJson + "}";
				}
			}
		}
		result = buildedJson;
		return result;
	}
}
