package local

import java.util.logging.Logger
import java.util.logging.Level
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

class DataDownloader {
	static final def LOGGER = Logger.getLogger(this.class.name)
	static final def XML_SLURPER = new XmlSlurper()

	def config
	def mapItemToItems
	def outputDir
	def yaml

	DataDownloader(config) {
		this.config = config
		this.mapItemToItems = [ : ]
	}
	
	def begin(itemType, itemId) {
		this.outputDir = new File(this.config.outputDirectory)
		this.outputDir.mkdirs()
		this.outputDir.eachFileRecurse { it.delete() }
		
		def dumperOptions = new DumperOptions()
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
		this.yaml = new Yaml(dumperOptions)

		this.mapItemToItems['root'] = "${itemType}:${itemId}".toString()
	}

	def fetch(itemType, itemId, level=0) {
		if (! (itemType in this.config.allowedItemTypes)) {
			return
		}

		if (level == 0) {
			this.begin(itemType, itemId)
		}
		LOGGER.log(Level.INFO, "Downloading: ${itemType}:${itemId} ...")
		
		def rootItem = this.config.remoteSystem.findItem(itemType, itemId)
		this.saveItem(itemType, itemId, rootItem)
		
		def subItemsRefs = this.findSubItemsRefsIn(rootItem)
		
		for (subItemRef in subItemsRefs) {
			this.fetch(subItemRef.type, subItemRef.id, level + 1)			

			this.mapItemToItems.computeIfAbsent("${itemType}:${itemId}".toString(), k -> [])
				<< "${subItemRef.type}:${subItemRef.id}".toString()
		}
		if (level == 0) {
			this.end()
		}
	}

	def findSubItemsRefsIn(item) {
		def definition = XML_SLURPER.parseText(item)
		def subItemsRefs = definition.'**'.findAll { node ->
				node.name() == 'item-ref'
			}.collect { node ->
				return [type: node.@type, id: node.@id]
			}
		return subItemsRefs
	}

	def saveItem(itemType, itemId, item) {
		new File("${this.outputDir}/${itemType}-${itemId}.xml") << item.trim()
	}
	
	def end() {
		new File("${this.outputDir}/index.yaml") << this.yaml.dump(mapItemToItems)
	}
}
