package local

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

public class FeatureParser {

	def structureBuilder = new local.MapStructureBuilder()
	def yamlContainers = [:]
	def dataSequence = 0
	def from
	def to
	def identifier
	def outputField

	def parse(featureFile) {
		def dumperOptions = new DumperOptions()
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
		def yaml = new Yaml(dumperOptions)

		new File(featureFile).eachLine { line ->
			this.assignLineToStructure(line)
		}
		yamlContainers.each { id, data ->
			def yamlSrc = data["yaml"]
			def map

			try {
				map = yaml.load(yamlSrc)
			}
			catch (Exception e) {
				throw new Exception("Error parsing Yaml data named [${id}]\n${e.getMessage()}${yamlSrc}")
			}
			data["yaml"] = "---\n${yaml.dump(map)}".toString()
		}
		structureBuilder.index()
		return structureBuilder.structure
	}

	def assignLineToStructure(line) {
		def m
	
		if (m = line =~ /^(Feature):\s+(.+)/) {
			def type = m[0][1]
			def description = m[0][2]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("/feature@ft/.@out",
					"@class", "gherkin.$type".toString(),
					outputField, description)
				.addEntries("@ft/examples@xmpls")
		}
		else if (m = line =~ /^\s*(Background):/) {
			def type = m[0][1]

			structureBuilder
				.addEntries("/feature/background@bck",
					"@class", "gherkin.$type".toString())
				.addEntries("@bck/steps@stps/*")
		}
		else if (m = line =~ /^\s*(Actor)\s+(\w+)\s+(.+)/) {
			def type = m[0][1]
			identifier = m[0][2]
			def description = m[0][3]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@bck/${identifier}/.@out",
					"@class", "extension.$type".toString(),
					"id", identifier,
					outputField, description)
		}
		else if (m = line =~ /^\s*(Participant)\s+(\w+)\s+(.+)/) {
			def type = m[0][1]
			identifier = m[0][2]
			def description = m[0][3]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@bck/${identifier}/.@out",
					"@class", "extension.$type".toString(),
					"id", identifier,
					outputField, description)
		}
		else if (m = line =~ /^\s*(Scenario):\s+(.+)/) {
			def type = m[0][1]
			def description = m[0][2]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@ft/scenarios@scns/+/.@out",
					"@class", "gherkin.$type".toString(),
					outputField, description)
				.addEntries("@scns/*/steps@stps/*")
				.addEntries("@scns/*/examples@xmpls")
		}
		else if (m = line =~ /^\s*(Given)\s+(.+)/) {
			def type = m[0][1]
			def description = m[0][2]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@stps/+/.@out",
					"@class", "gherkin.$type".toString(),
					outputField, description)
		}
		else if (m = line =~ /^\s*(When)\s+(\w+?),(\w+?)\s+(.+)/) {
			def type = m[0][1]
			from = m[0][2] == null ? from : m[0][2]
			to = m[0][3] == null ? to : m[0][3]
			def description = m[0][4]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@stps/+/.@out",
					"@class", "gherkin.$type".toString(),
					"from", from,
					"to", to,
					outputField, description)
		}
		else if (m = line =~ /^\s*(Then)\s+(\w+?),(\w+?)(\s+(.+))?/) {
			def type = m[0][1]
			from = m[0][2] == null ? from : m[0][2]
			to = m[0][3] == null ? to : m[0][3]
			def description = m[0][5]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@stps/+/.@out",
					"@class", "gherkin.$type".toString(),
					"from", from,
					"to", to,
					outputField, description)
		}
		else if (m = line =~ /^\s*(And)\s+((\w+?),(\w+?)\s+)?(.+)/) {
			def type = m[0][1]
			from = m[0][3] == null ? to : m[0][3]
			to = m[0][4] == null ? to : m[0][4]
			def description = m[0][5]
			description = description == null ? "" : description.trim()
			outputField = "description"

			structureBuilder
				.addEntries("@stps/+/.@out",
					"@class", "gherkin.$type".toString(),
					"from", from,
					"to", to,
					outputField, description)
		}
		else if (m = line =~ /^\s*(Examples):/) {
			//  NOOP
		}
		else if (m = line =~ /^\s*(Data)\s+(\w+)\s+(.+)/) {
			def type = m[0][1]
			identifier = m[0][2]
			def description = m[0][3]
			description = description == null ? "" : description.trim()
			outputField = "description"

			this.dataSequence++

			structureBuilder
				.addEntries("@xmpls/${identifier}@dt/.@out",
					"@class", "extension.$type".toString(),
					"id", identifier,
					outputField, description)
		}
		else if (m = line =~ /^\s*---\s*$/) {
			def key = "#${this.dataSequence}::${identifier}"
			outputField = "yaml"

			structureBuilder
				.addEntries("@dt/.@out",
					outputField, line.trim())

			yamlContainers[key] = structureBuilder.getValue("@dt")
		}
		else {
			if (outputField != null) {
				def content = structureBuilder.getValue("@out/${outputField}", "")
				content = line.trim() == "" ? content : content + "\n" + line

				structureBuilder
					.addEntries("@out",
						outputField, content)
			}
		}
	}
}
