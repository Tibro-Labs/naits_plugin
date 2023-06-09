package naits_triglav_plugin;

import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.DateTime;

import com.prtech.svarog.SvConf;
import com.prtech.svarog.SvException;
import com.prtech.svarog.SvReader;
import com.prtech.svarog_common.DbDataArray;
import com.prtech.svarog_common.DbDataObject;

public class PopulationQueryBuilder {
	/**
	 * Method for building simple query (select-from-where)
	 * 
	 * @param tableName
	 * @param tableInstanceName
	 * @param filterColumnNameValue
	 * @param selectColumns
	 * @return
	 */
	public static String buildSimpleQuery(String tableName, String tableInstanceName,
			Map<String, Object> filterColumnNameValue, String... selectColumns) {
		StringBuilder queryResult = new StringBuilder();
		StringBuilder select = new StringBuilder("SELECT ");
		StringBuilder from = new StringBuilder(" FROM ");
		StringBuilder where = new StringBuilder(" WHERE ");
		for (int i = 0; i < selectColumns.length; i++) {
			select.append(selectColumns[i]);
			if (i != selectColumns.length - 1) {
				select.append(", ");
			}
		}
		from.append(tableName).append(" ").append(tableInstanceName);
		int i = 0;
		for (Entry<String, Object> field : filterColumnNameValue.entrySet()) {
			String value = "";
			if (field.getValue() instanceof String) {
				value = "'" + field.getValue() + "'";
			} else {
				value = String.valueOf(field.getValue());
			}
			where.append(field.getKey()).append("=").append(value);
			if (i != filterColumnNameValue.size() - 1) {
				where.append(" AND ");
			}
			i++;
		}
		queryResult.append(select).append(from).append(where).append(";");
		return queryResult.toString();
	}

	/**
	 * Method for building simple query with sub-query (select-from-where-in/not
	 * in-subquery). This method is used for Population module
	 * 
	 * @param tableName
	 * @param tableInstanceName
	 * @param columnName
	 * @param inNotIn
	 * @param subQuery
	 * @param selectColumns
	 * @return
	 */
	public static String buildSimpleQueryWithSubquery(String tableName, String columnName, String inNotIn,
			StringBuilder subQuery, String... selectColumns) {
		StringBuilder queryResult = new StringBuilder();
		StringBuilder select = new StringBuilder("SELECT ");
		StringBuilder from = new StringBuilder(" FROM ");
		StringBuilder where = new StringBuilder(" WHERE ");
		for (int i = 0; i < selectColumns.length; i++) {
			select.append(selectColumns[i]);
			if (i != selectColumns.length - 1) {
				select.append(", ");
			}
		}
		from.append(tableName).append(" ").append(" ");
		where.append(columnName).append(" ").append(inNotIn).append(" (").append(subQuery);
		queryResult.append(select).append(from).append(where).append(")");
		return queryResult.toString();
	}

	public static StringBuilder buildQueryAccordingGeolocationFilters(DbDataObject dboPopulation, SvReader svr)
			throws SvException {
		StringBuilder query = new StringBuilder();
		DbDataObject dboLinkTypeAreaPopulation = SvReader.getLinkType(Tc.AREA_POPULATION,
				SvReader.getTypeIdByName(Tc.AREA), SvReader.getTypeIdByName(Tc.POPULATION));
		DbDataArray arrLinkedGeolocations = svr.getObjectsByLinkedId(dboPopulation.getObject_id(),
				dboPopulation.getObject_type(), dboLinkTypeAreaPopulation, SvReader.getTypeIdByName(Tc.AREA), true,
				new DateTime(), 0, 0);
		if (!arrLinkedGeolocations.getItems().isEmpty()) {
			query.append(" AND (");
			for (int i = 0; i < arrLinkedGeolocations.size(); i++) {
				DbDataObject dboArea = arrLinkedGeolocations.get(i);
				if (dboArea.getVal(Tc.AREA_TYPE) != null) {
					if (dboArea.getVal(Tc.AREA_TYPE).toString().equals("0")) {
						query.append(" ").append(Tc.REGION_CODE).append("=").append("'")
								.append(dboArea.getVal(Tc.AREA_NAME).toString()).append("'");
					} else if (dboArea.getVal(Tc.AREA_TYPE).toString().equals("1")) {
						query.append(" ").append(Tc.MUNIC_CODE).append("=").append("'")
								.append(dboArea.getVal(Tc.AREA_NAME).toString()).append("'");
					} else if (dboArea.getVal(Tc.AREA_TYPE).toString().equals("2")) {
						query.append(" ").append(Tc.COMMUN_CODE).append("=").append("'")
								.append(dboArea.getVal(Tc.AREA_NAME).toString()).append("'");
					} else if (dboArea.getVal(Tc.AREA_TYPE).toString().equals("3")) {
						query.append(" ").append(Tc.VILLAGE_CODE).append("=").append("'")
								.append(dboArea.getVal(Tc.AREA_NAME).toString()).append("'");
					}
				}
				if (i < arrLinkedGeolocations.size() - 1) {
					query.append(" OR");
				}
			}
			query.append(") ");
		}
		return query;
	}

	/**
	 * Method for building query according population input. Population input is
	 * used as filter/s so based on it, we build query where we fetch data from
	 * materialized views build for population module's purpose
	 * 
	 * @param dboPopulation
	 * @return
	 * @throws SvException
	 */
	public static StringBuilder getQueryAccordingPopulationFilters(DbDataObject dboPopulation) throws SvException {
		StringBuilder result = new StringBuilder();
		String schema = SvConf.getDefaultSchema();
		if (dboPopulation != null) {
			String apdf = schema + ".animal_population_disease_filter apdf";
			String apif = schema + ".animal_population_inventory_filter apif";
			String apsf = schema + ".animal_population_sample_filter apsf";
			String apvbf = schema + ".animal_population_vacc_book_filter apvbf";
			String hpaf = schema + ".holding_population_age_filter hpaf";
			String hpgf = schema + ".holding_population_gender_filter hpgf";
			String hpatf = schema + ".holding_population_ani_type_filter hpatf";
			String htacs = schema + ".holding_total_animal_count_by_specie htacs";
			String excludeSetFlag = "";

			StringBuilder query = new StringBuilder("SELECT DISTINCT HPAF.PKID FROM ");
			StringBuilder whereClause = new StringBuilder(" WHERE");

			// Age filter
			query.append(hpaf);
			if (dboPopulation.getVal(Tc.FILTER_AGE_FROM) == null && dboPopulation.getVal(Tc.FILTER_AGE_TO) == null) {
				whereClause.append(" HPAF.AGE_MONTHS >= 0");
			} else {
				boolean isAgeExcluded = false;
				if (dboPopulation.getVal(Tc.AGE_FILTER) != null
						&& dboPopulation.getVal(Tc.AGE_FILTER).toString().equals(Tc.EXCLUDE)) {
					isAgeExcluded = true;
					excludeSetFlag = "1";
					whereClause.append(" NOT EXISTS ( ").append("SELECT PKID FROM ").append(hpaf).append(excludeSetFlag)
							.append(" WHERE ");
				}
				if (dboPopulation.getVal(Tc.FILTER_AGE_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_AGE_FROM) != null) {
					whereClause.append(" HPAF").append(excludeSetFlag).append(".AGE_MONTHS BETWEEN ")
							.append(dboPopulation.getVal("FILTER_AGE_FROM").toString()).append(" AND ")
							.append(dboPopulation.getVal("FILTER_AGE_TO").toString());
				} else if (dboPopulation.getVal(Tc.FILTER_AGE_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_AGE_FROM) == null) {
					whereClause.append(" HPAF").append(excludeSetFlag).append(".AGE_MONTHS <= ")
							.append(dboPopulation.getVal(Tc.FILTER_AGE_TO).toString());
				} else if (dboPopulation.getVal(Tc.FILTER_AGE_TO) == null
						&& dboPopulation.getVal(Tc.FILTER_AGE_FROM) != null) {
					whereClause.append(" HPAF").append(excludeSetFlag).append(".AGE_MONTHS >= ")
							.append(dboPopulation.getVal(Tc.FILTER_AGE_FROM).toString());
				}
				if (isAgeExcluded) {
					whereClause.append(" AND HPAF.PKID=PKID ) ");
				}
			}
			// Gender filter
			if (dboPopulation.getVal(Tc.FILTER_GENDER) != null) {
				query.append(" JOIN ").append(hpgf).append(" ON HPAF.OBJECT_ID=HPGF.OBJECT_ID");
				whereClause.append(" AND HPGF.GENDER='").append(dboPopulation.getVal("FILTER_GENDER").toString())
						.append("'");
			}

			// SPECIE_COUNT FILTER
			if (dboPopulation.getVal(Tc.FILTER_ANI_TYPE) != null
					&& dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT) != null) {
				query.append(" JOIN ").append(hpatf).append(" ON HPAF.OBJECT_ID=HPATF.OBJECT_ID");
				query.append(" JOIN ").append(htacs).append(" ON HPAF.PARENT_ID=HTACS.OBJECT_ID");
				whereClause.append(" AND HPATF.ANIMAL_CLASS='")
						.append(dboPopulation.getVal(Tc.FILTER_ANI_TYPE).toString()).append("'");
				switch (dboPopulation.getVal(Tc.FILTER_ANI_TYPE).toString()) {
				case "1":
					whereClause.append(" AND HTACS.TOTAL_CATTLE >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				case "2":
					whereClause.append(" AND HTACS.TOTAL_BUFFOLO >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				case "9":
					whereClause.append(" AND HTACS.TOTAL_SHEEP >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				case "10":
					whereClause.append(" AND HTACS.TOTAL_GOAT >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				case "11":
					whereClause.append(" AND HTACS.TOTAL_PIG >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				case "400":
					whereClause.append(" AND HTACS.TOTAL_DONKEY >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT).toString());
					break;
				default:
					break;
				}
			}
			// ANIMAL_TYPE filter
			if (dboPopulation.getVal(Tc.FILTER_ANI_TYPE) != null
					&& dboPopulation.getVal(Tc.FILTER_MIN_TOTAL_CNT) == null) {
				query.append(" JOIN ").append(hpatf).append(" ON HPAF.OBJECT_ID=HPGF.OBJECT_ID");
				whereClause.append(" AND HPGF.ANIMAL_CLASS='")
						.append(dboPopulation.getVal(Tc.FILTER_ANI_TYPE).toString()).append("'");
			}
			// Health record book filter
			if (dboPopulation.getVal(Tc.VACC_BOOK_FILTER) != null) {
				query.append(" JOIN ").append(apvbf).append(" ON HPAF.OBJECT_ID=APVBF.OBJECT_ID ");
				whereClause.append(" AND ");
				boolean isVaccExcluded = false;
				if (dboPopulation.getVal(Tc.VACC_BOOK_FILTER).toString().equals(Tc.EXCLUDE)) {
					excludeSetFlag = "1";
					isVaccExcluded = true;
					whereClause.append(" NOT EXISTS ( ").append("SELECT PKID FROM ").append(apvbf)
							.append(excludeSetFlag).append(" WHERE ");
				}
				if (dboPopulation.getVal(Tc.VACC_BOOK_TO) != null && dboPopulation.getVal(Tc.VACC_BOOK_FROM) != null) {
					whereClause.append(" APVBF.AGE_MONTHS BETWEEN ")
							.append(dboPopulation.getVal(Tc.VACC_BOOK_FROM).toString()).append(" AND ")
							.append(dboPopulation.getVal(Tc.VACC_BOOK_TO).toString());
				} else if (dboPopulation.getVal(Tc.VACC_BOOK_TO) != null
						&& dboPopulation.getVal(Tc.VACC_BOOK_FROM) == null) {
					whereClause.append(" APVBF.AGE_MONTHS <= ")
							.append(dboPopulation.getVal(Tc.VACC_BOOK_TO).toString());
				} else if (dboPopulation.getVal(Tc.VACC_BOOK_TO) == null
						&& dboPopulation.getVal(Tc.VACC_BOOK_FROM) != null) {
					whereClause.append(" APVBF.AGE_MONTHS >= ")
							.append(dboPopulation.getVal(Tc.VACC_BOOK_FROM).toString());
				}
				if (dboPopulation.getVal(Tc.DISEASE_VACC_BOOK) != null) {
					whereClause.append(" AND APVBF.DISEASE_BOOK IN (")
							.append(getVaccinationCodeInVaccinationBookAccordingDiseaseCode(
									dboPopulation.getVal(Tc.DISEASE_VACC_BOOK).toString()))
							.append(")");
				}
				if (isVaccExcluded) {
					whereClause.append(" AND APVBF.PKID=PKID ) ");
				}

			}
			// Campaign filter
			if (dboPopulation.getVal(Tc.VACCINE_FILTER) != null) {
				query.append(" JOIN ").append(apdf).append(" ON HPAF.OBJECT_ID=APDF.OBJECT_ID ");
				whereClause.append(" AND ");
				boolean isVaccExcluded = false;
				if (dboPopulation.getVal(Tc.VACCINE_FILTER).toString().equals(Tc.EXCLUDE)) {
					excludeSetFlag = "1";
					isVaccExcluded = true;
					whereClause.append(" NOT EXISTS ( ").append("SELECT PKID FROM ").append(apdf).append(excludeSetFlag)
							.append(" WHERE ");
				}
				// check if campaign is included
				if (dboPopulation.getVal(Tc.CAMPAIGN_OBJ_ID) != null) {
					whereClause.append(" DISEASE='")
							.append(dboPopulation.getVal(Tc.DISEASE_VACCINATION).toString() + "'").append(" AND ")
							.append(Tc.CAMPAIGN_OBJ_ID).append("=").append("'")
							.append(dboPopulation.getVal(Tc.CAMPAIGN_OBJ_ID).toString()).append("'");
				}
				if (isVaccExcluded) {
					whereClause.append(" AND HPAF.PKID=PKID ) ");
				}
			}
			// Positive/Inconclusive filter
			if (dboPopulation.getVal(Tc.SAMPLE_FILTER) != null) {
				query.append(" JOIN ").append(apsf).append(" ON HPAF.OBJECT_ID=APSF.OBJECT_ID");
				whereClause.append(" AND ");
				boolean isSampleExcluded = false;
				if (dboPopulation.getVal(Tc.SAMPLE_FILTER).toString().equals(Tc.EXCLUDE)) {
					excludeSetFlag = "1";
					isSampleExcluded = true;
					whereClause.append(" NOT EXISTS ( ").append("SELECT PKID FROM ").append(apsf).append(excludeSetFlag)
							.append(" WHERE ");
				}
				if (dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_FROM) != null) {
					whereClause.append(" APSF").append(excludeSetFlag).append(".AGE_MONTHS BETWEEN ")
							.append(dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_FROM).toString()).append(" AND ")
							.append(dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_TO).toString());
				} else if (dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_FROM) == null) {
					whereClause.append(" APSF").append(excludeSetFlag).append(".AGE_MONTHS <= ")
							.append(dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_TO).toString());
				} else if (dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_TO) == null
						&& dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_FROM) != null) {
					whereClause.append(" APSF").append(excludeSetFlag).append(".AGE_MONTHS >= ")
							.append(dboPopulation.getVal(Tc.FILTER_HEALTH_CHECK_FROM).toString());
				}
				whereClause.append(" AND DISEASE_TEST='")
						.append(dboPopulation.getVal("DISEASE_STATUS").toString() + "'");
				if (isSampleExcluded) {
					whereClause.append(" AND HPAF.PKID=PKID ) ");
				}
			}
			// Inventory item/Ear tag filter
			if (dboPopulation.getVal(Tc.EAR_TAG_FILTER) != null) {
				query.append(" JOIN ").append(apif).append(" ON HPAF.OBJECT_ID=APIF.OBJECT_ID");
				whereClause.append(" AND ");
				boolean isEarTagExcluded = false;
				if (dboPopulation.getVal(Tc.EAR_TAG_FILTER).toString().equals(Tc.EXCLUDE)) {
					excludeSetFlag = "1";
					isEarTagExcluded = true;
					whereClause.append(" NOT EXISTS ( ").append("SELECT PKID FROM ").append(apif).append(excludeSetFlag)
							.append(" WHERE ");
				}
				if (dboPopulation.getVal(Tc.FILTER_MISSING_TAG_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_MISSING_TAG_FROM) != null) {
					whereClause.append(" APIF").append(excludeSetFlag).append(".AGE_MONTHS BETWEEN ")
							.append(dboPopulation.getVal(Tc.FILTER_MISSING_TAG_FROM).toString()).append(" AND ")
							.append(dboPopulation.getVal(Tc.FILTER_MISSING_TAG_TO).toString());
				} else if (dboPopulation.getVal(Tc.FILTER_MISSING_TAG_TO) != null
						&& dboPopulation.getVal(Tc.FILTER_MISSING_TAG_FROM) == null) {
					whereClause.append(" APIF").append(excludeSetFlag).append(".AGE_MONTHS <= ")
							.append(dboPopulation.getVal(Tc.FILTER_MISSING_TAG_TO).toString());
				} else if (dboPopulation.getVal(Tc.FILTER_MISSING_TAG_TO) == null
						&& dboPopulation.getVal(Tc.FILTER_MISSING_TAG_FROM) != null) {
					whereClause.append(" APIF").append(excludeSetFlag).append(".AGE_MONTHS >= ")
							.append(dboPopulation.getVal(Tc.FILTER_MISSING_TAG_FROM).toString());
				}
				if (isEarTagExcluded) {
					whereClause.append(" AND HPAF.PKID=PKID ) ");
				}
			}
			result.append(query).append(whereClause);
		}
		return result;
	}

	/**
	 * Temporary method for getting vaccine code values according disease. This need
	 * to be replaced appropriately after we get the full list of diseases for
	 * vaccination book
	 * 
	 * @param diseaseCode
	 * @param svr
	 * @return
	 */
	public static String getVaccinationCodeInVaccinationBookAccordingDiseaseCode(String diseaseCode) {
		String result = "'100'";
		switch (diseaseCode) {
		case "1":
			result = "'FMD-1', 'FMD-2', 'FMD-RE', 'FMD-FORCE'";
			break;
		case "2":
			result = "'ANTHR-GOV', 'ANTHR-RE', 'ANTHR-FORCE'";
			break;
		case "3":
			result = "'BRUC-GOV', 'BRUC-RE', 'BRUC-FORCE'";
			break;
		case "5":
			result = "'RABIES-GOV', 'RABIES-RE', 'RABIES-FORCE'";
			break;
		default:
			break;
		}
		return result;
	}
}
