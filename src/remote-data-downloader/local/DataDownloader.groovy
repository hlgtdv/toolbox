package local

import java.util.logging.*

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

	DataDownloader(config) {
		this.config = config
		this.workDir = new File(this.config.workDirectory)
		this.outputDir = new File(this.config.outputDirectory)
		this.mapItemToItems = [ : ]
		
		this.init()
	}
	
	def init() {
		this.workDir.mkdirs()
		this.outputDir.mkdirs()
		this.workDir.eachFileRecurse { it.delete() }
		this.outputDir.eachFileRecurse { it.delete() }
	}

	def fetch(itemType, itemId, isFetchSubItems = false) {
		def itemRef = new Ref(itemType, itemId)

		LOGGER.log(Level.INFO, "Downloading: ${itemRef} ...")
		
		def rootItem = this.config.remoteSystem.findItem(itemType, itemId)
		this.saveItem(itemRef, rootItem)
		
		def subItemsRefs = this.findSubItemsRefsIn(rootItem)
		
		for (subItemRef in subItemsRefs) {				
			this.fetch(subItemRef.type, subItemRef.id, itemType in this.config.allowedItemTypes)
			
			this.mapItemToItems.computeIfAbsent(itemRef, k -> []) << subItemRef			
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
}
