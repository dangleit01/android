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

public class DiagrammDataChartCoordinate {
	protected BigDecimal xValue;
	protected BigDecimal yValue;

	protected Object xValueObject;
	
	public DiagrammDataChartCoordinate(BigDecimal xValue, BigDecimal yValue, Object xValueObject) {
		super();
		this.xValue = xValue;
		this.yValue = yValue;
		this.xValueObject = xValueObject;
	}

	public DiagrammDataChartCoordinate() {
	}

	public BigDecimal getxValue() {
		return xValue;
	}

	public void setxValue(BigDecimal xValue) {
		this.xValue = xValue;
	}

	public BigDecimal getyValue() {
		return yValue;
	}

	public void setyValue(BigDecimal yValue) {
		this.yValue = yValue;
	}

	public Object getxValueObject() {
		return xValueObject;
	}

	public void setxValueObject(Object xValueObject) {
		this.xValueObject = xValueObject;
	}
}
