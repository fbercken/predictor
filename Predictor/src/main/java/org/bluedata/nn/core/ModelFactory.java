package org.bluedata.nn.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ModelFactory {

	public abstract void writeModel( Model model, OutputStream outputStream) throws IOException;
	
	public abstract Model readModel(InputStream input) throws IOException;
	
}
