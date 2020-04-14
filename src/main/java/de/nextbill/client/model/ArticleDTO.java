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

package de.nextbill.client.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class ArticleDTO implements Serializable {

	private String id;

	private String name;
	private BigDecimal price;

	private BigDecimal startX;
	private BigDecimal endX;
	private BigDecimal startY;
	private BigDecimal endY;

	public ArticleDTO() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getStartX() {
		return startX;
	}

	public void setStartX(BigDecimal startX) {
		this.startX = startX;
	}

	public BigDecimal getEndX() {
		return endX;
	}

	public void setEndX(BigDecimal endX) {
		this.endX = endX;
	}

	public BigDecimal getStartY() {
		return startY;
	}

	public void setStartY(BigDecimal startY) {
		this.startY = startY;
	}

	public BigDecimal getEndY() {
		return endY;
	}

	public void setEndY(BigDecimal endY) {
		this.endY = endY;
	}
}
