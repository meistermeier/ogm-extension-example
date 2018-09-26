package org.neo4j.ogm.extensionsample;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class ConfigurationNode {
  private String value;
  private Long id;

  ConfigurationNode(String value) {
	this.value = value;
  }

  public String getValue() {
	return value;
  }

  @Override public String toString() {
	return "ConfigurationNode{" +
		"id=" + id + '\'' +
		", value='" + value +
		'}';
  }
}
