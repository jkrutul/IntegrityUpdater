package com.ptc.integrity.services.utilities.models;

public class Sandbox {
	private String sandboxName, serverName, projectName;
	
	public Sandbox(){
		
	}

	public String getSandboxName() {
		return sandboxName;
	}

	public void setSandboxName(String sandboxName) {
		this.sandboxName = sandboxName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
}
