package local

import java.util.logging.*

import org.yaml.snakeyaml.Yaml

class DataDownloader {

	class Ref {
		def type
		def id

		Ref(type, id) {			
			this.type = type
			this.id = id
		}
		
		String toString() {
			return "${this.type}:${this.id}"
		}

		String toFilename() {
			return "${this.type}-${this.id}.xml"
		}
	}

	static final def LOGGER = Logger.getLogger(this.class.name)
	static final def XML_SLURPER = new XmlSlurper()

	def config
	def workDir
	def outputDir
	def mapItemToItems
	def yaml

	DataDownloader(config) {
		this.config = config
		this.workDir = new File(this.config.workDirectory)
		this.outputDir = new File(this.config.outputDirectory)
		this.mapItemToItems = [ : ]
		this.yaml = new Yaml()
	}
	
	def begin(itemRef) {
		this.workDir.mkdirs()
		this.outputDir.mkdirs()
		this.workDir.eachFileRecurse { it.delete() }
		this.outputDir.eachFileRecurse { it.delete() }
		
		this.mapItemToItems['root'] = itemRef.toString()
	}

	def fetch(itemType, itemId, level=0) {
		if (! (itemType in this.config.allowedItemTypes)) {
			return
		}
		def itemRef = new Ref(itemType, itemId)

		if (level == 0) {
			this.begin(itemRef)
		}

		LOGGER.log(Level.INFO, "Downloading: ${itemRef} ...")
		
		def rootItem = this.config.remoteSystem.findItem(itemType, itemId)
		this.saveItem(itemRef, rootItem)
		
		def subItemsRefs = this.findSubItemsRefsIn(rootItem)
		
		for (subItemRef in subItemsRefs) {
			this.fetch(subItemRef.type, subItemRef.id, level + 1)
			
			this.mapItemToItems.computeIfAbsent(itemRef.toString(), k -> []) << subItemRef.toString()			
		}
		if (level == 0) {
			this.end()
		}
	}

	def saveItem(itemRef, item) {
		new File("${this.workDir}/${itemRef.toFilename()}") << item.trim()
	}
	
	def findSubItemsRefsIn(item) {
		def definition = XML_SLURPER.parseText(item)
		def subItemsRefs = definition.'**'.findAll { node ->
				node.name() == 'item-ref'
			}.collect { node ->
				return new Ref(node.@type, node.@id)
			}

		return subItemsRefs
	}

	def end() {
		new File("${this.workDir}/index.yaml") << this.yaml.dump(mapItemToItems)
	}
}
