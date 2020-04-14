/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.pojos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DiagrammDataChart {

	public DiagrammDataChart() {
	}

	protected List<DiagrammDataChartCoordinate> coordinates;
	protected String displayName;
	protected String xAxeDisplayName;
	protected String yAxeDisplayName;
	protected String description;
	protected Map<BigDecimal, String> xAxesValues;
	boolean sortByYValues = true;

	public List<DiagrammDataChartCoordinate> getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(List<DiagrammDataChartCoordinate> coordinates) {
		this.coordinates = coordinates;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getxAxeDisplayName() {
		return xAxeDisplayName;
	}

	public void setxAxeDisplayName(String xAxeDisplayName) {
		this.xAxeDisplayName = xAxeDisplayName;
	}

	public String getyAxeDisplayName() {
		return yAxeDisplayName;
	}

	public void setyAxeDisplayName(String yAxeDisplayName) {
		this.yAxeDisplayName = yAxeDisplayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<BigDecimal, String> getxAxesValues() {
		return xAxesValues;
	}

	public void setxAxesValues(Map<BigDecimal, String> xAxesValues) {
		this.xAxesValues = xAxesValues;
	}

	public boolean isSortByYValues() {
		return sortByYValues;
	}

	public void setSortByYValues(boolean sortByYValues) {
		this.sortByYValues = sortByYValues;
	}
}
