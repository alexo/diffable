package com.google.diffable.scripts;

import com.google.inject.Singleton;

@Singleton
public class DiffableClientTemplate extends DiffableTemplate {
	public DiffableClientTemplate() {
		this.setTemplate("/com/google/diffable/scripts/DiffableClient.min.js");
	}
}
