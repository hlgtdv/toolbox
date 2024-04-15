package local

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

class RepositoryDocumentator {

	class Util {
		
		static def method() {
			
			return "unimplemented method"
		}
	}

	def config
	def yaml
	def configEntries

	RepositoryDocumentator(config) {
		this.config = config
		this.configEntries = [:]
	}
	
	def run() {
		init()

		new File(config.inputDirectory).eachFile { file ->
			processStructureFile(file)
		}
	}
	
	def init() {
		def dumperOptions = new DumperOptions()
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
		this.yaml = new Yaml(dumperOptions)

		new File(config.configDirectory).eachFile { file ->
			loadConfigFile(file)
		}
	}
	
	def loadConfigFile(file) {
		println "[ DOC] Loading configuration file: $file..."

		def conf = this.yaml.load(file.text)
		
		this.configEntries[conf.'on-documentate'.type] = conf		
	}

	def processStructureFile(file) {
		println "[ DOC] Processing structure file: $file..."

		def mapStructure = this.yaml.load(file.text)
		
		mapStructure.each { key, element ->
			processElement(key, element)
		}
	}

	def processElement(key, element) {
		println "[ DOC] Processing element: $key..."

		def metaClass = element.attributes.'@class'
		def directives = this.configEntries[metaClass]
		
		if (directives == null) {
			println "[ DOC]	SKIPPED: configuration not found for [$metaClass]"
		}
		else {
			println directives
			
			// TEST Templates
			def values = [ "a" : [ [ "b" : "3" ], [ "c" : "2" ] ] ]
			def engine = new groovy.text.GStringTemplateEngine()
			def text = '''Salut:
<%	for (item in values) { %>\
<%		if (item != "2") { %>\
<%			def a = 123 %>
${values.a[1].c}
${a}
${utl.method()}
<% 		} %>\
<%	} %>\
			'''.trim()
			println engine.createTemplate(text).make([values: values, utl: Util])			
		}
	}
}
