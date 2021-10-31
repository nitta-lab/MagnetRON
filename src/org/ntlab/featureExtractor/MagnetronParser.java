package org.ntlab.featureExtractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MagnetronParser {
	public static Map<String, Object> doParse(BufferedReader reader) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket, ExpectedFeatures, ExpectedExtracts {
		return parseMagnetron(reader);
	}
	
	private static Map<String, Object> parseMagnetron(BufferedReader reader) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket, ExpectedFeatures, ExpectedExtracts {
		Map<String, Object> magnet = new HashMap<>();
		String line = skipBlankLines(null, reader);
		if (line == null || !line.startsWith("{")) {
			throw new ExpectedLeftCurlyBraket();
		}
		line = line.substring(1);
		line = parseKeyValue(line, reader, magnet);		// Trace file name
		if (line == null || !line.startsWith(",")) throw new ExpectedFeatures();
		line = line.substring(1);
		
		String keys[] = new String[1];
		line = parseKey(line, reader, keys);
		if (line == null) new ExpectedFeatures();
		if (keys[0].equals("format")) {
			// Trace file format
			Object[] values = new Object[1];
			line = parseValue(line, reader, values);
			magnet.put("format", values[0]);	// JSON or PlainText
			if (line == null || !line.startsWith(",")) throw new ExpectedFeatures();
			line = line.substring(1);
			line = parseKey(line, reader, keys);			
			if (line == null) new ExpectedFeatures();
		} else {
			// JSON format is default.
			magnet.put("format", "JSON");
		}
		// Features in the trace file
		if (!keys[0].equals("features")) new ExpectedFeatures();
		List<Feature> freatures = new ArrayList<>();
		line = parseFeatures(line, reader, freatures);
		magnet.put("features", freatures);
		return magnet;
	}

	private static String parseFeatures(String residual, BufferedReader reader, List<Feature> freatures) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket, ExpectedExtracts {
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("[")) {
			throw new ExpectedLeftCurlyBraket();
		}
		residual = residual.substring(1);
		do {
			residual = parseFeature(residual, reader, freatures);
			residual = skipBlankLines(residual, reader);
			if (residual.startsWith(",")) {
				residual = residual.substring(1);
				continue;
			}
		} while (!residual.startsWith("]"));
		return residual.substring(1);
	}
	
	private static String parseFeature(String residual, BufferedReader reader, List<Feature> freatures) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket, ExpectedExtracts {
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("{")) {
			throw new ExpectedLeftCurlyBraket();
		}
		residual = residual.substring(1);
		
		Feature feature = new Feature();		
		Map<String, Object> keyValue = new HashMap<>();
		residual = parseKeyValue(residual, reader, keyValue);		// Feature name
		feature.setName((String) keyValue.get("feature"));
		
		if (residual == null || !residual.startsWith(",")) throw new ExpectedExtracts();
		residual = residual.substring(1);
		
		String keys[] = new String[1];
		residual = parseKey(residual, reader, keys);					// Features in the trace file
		if (residual == null || !keys[0].equals("extracts")) new ExpectedExtracts();
		List<Extract> extracts = new ArrayList<>();
		residual = parseExtracts(residual, reader, extracts);
		feature.setExtracts(extracts);
		freatures.add(feature);
		
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("}")) {
			throw new ExpectedRightCurlyBraket();
		}
		return residual.substring(1);
	}

	private static String parseExtracts(String residual, BufferedReader reader, List<Extract> extracts) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("[")) {
			throw new ExpectedLeftCurlyBraket();
		}
		residual = residual.substring(1);
		do {
			residual = parseExtract(residual, reader, extracts);
			residual = skipBlankLines(residual, reader);
			if (residual.startsWith(",")) {
				residual = residual.substring(1);
				continue;
			}
		} while (!residual.startsWith("]"));
		return residual.substring(1);
	}
	
	private static String parseExtract(String residual, BufferedReader reader, List<Extract> extracts) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("{")) {
			throw new ExpectedLeftCurlyBraket();
		}
		
		Map<String, Object> object = new HashMap<>();		
		residual = parseObject(residual, reader, object);
		Map<String, Object> srcObject = (Map<String, Object>) object.get("src");
		Map<String, Object> dstObject = (Map<String, Object>) object.get("dst");
		int order = Integer.parseInt((String) object.get("order"));
		boolean isToConnect = false;
		if (object.get("connect") != null) {
			isToConnect = (Boolean) object.get("connect");
		}
		Extract extract = new Extract(
				(String) srcObject.get("id"), 
				(String) srcObject.get("class"), 
				(String) dstObject.get("id"), 
				(String) dstObject.get("class"), 
				(String) object.get("type"), 
				order, isToConnect);
		extracts.add(extract);		
		return residual;
	}

	private static String parseObject(String residual, BufferedReader reader, Map<String, Object>object)  throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		residual = skipBlankLines(residual, reader);
		if (residual == null || !residual.startsWith("{")) {
			throw new ExpectedLeftCurlyBraket();
		}
		residual = residual.substring(1);
		do {
			residual = parseKeyValue(residual, reader, object);
			if (residual == null) throw new ExpectedRightCurlyBraket();
			residual = skipBlankLines(residual, reader);
			if (residual.startsWith(",")) {
				residual = residual.substring(1);
				continue;
			}
		} while (!residual.startsWith("}"));
		return residual.substring(1);
	}
	
	private static String parseKey(String residual, BufferedReader reader, String[] keys) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		String line = skipBlankLines(residual, reader);
		if (line == null) return null;
		line = line.trim();
		if (!line.startsWith("\"")) return null;
		String strings[] = line.split("\"");
		if (strings.length < 1) return null;
		String key = strings[1];
		if (line.indexOf(":") < 0) return null;
		residual = line.substring(line.indexOf(":") + 1);
		keys[0] = key;
		return residual;
	}

	private static String parseKeyValue(String residual, BufferedReader reader, Map<String, Object> object) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		String keys[] = new String[1];
		residual = parseKey(residual, reader, keys);
		if (residual == null) return null;
		Object[] values = new Object[1];
		residual = parseValue(residual, reader, values);
		object.put(keys[0], values[0]);
		return residual;
	}

	private static String parseValue(String residual, BufferedReader reader, Object[] values) throws IOException, ExpectedLeftCurlyBraket, ExpectedRightCurlyBraket {
		String line = skipBlankLines(residual, reader);
		line = line.trim();
		if (line.startsWith("\"")) {
			line = line.substring(1);
			values[0] = line.substring(0, line.indexOf("\""));
			residual = line.substring(line.indexOf("\"") + 1);
		} else if (line.startsWith("{")) {
			Map<String, Object>object = new HashMap<>();
			residual = parseObject(line, reader, object);
			values[0] = object;
		} else if (line.startsWith("[")) {
			// To Do
		} else if (line.startsWith("null")) {
			residual = line.substring(4);
			values[0] = null;
		} else if (line.startsWith("true")) {
			residual = line.substring(4);
			values[0] = true;
		} else if (line.startsWith("false")) {
			residual = line.substring(5);
			values[0] = false;
		} else {
			// To Do
		}
		return residual;
	}

	private static String skipBlankLines(String residual, BufferedReader reader) throws IOException {
		String line = residual;
		if (line == null || line.equals("")) {
			do  {
				line = reader.readLine();
				if (line == null) break;
				line = line.trim();
			} while (line.equals(""));
		} else {
			line = line.trim();			
		}
		return line;
	}
}
