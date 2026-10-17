package com.gimcompanion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One item stack in a drop. {@code value} is the Grand Exchange price of the whole stack. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DropItem
{
	private int itemId;
	private String name;
	private int qty;
	private long value;
}
