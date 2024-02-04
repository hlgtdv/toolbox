package local

class DataDownloader {

	class Ref {
		
		def type
		def id

		Ref(type, id) {
			
			this.type = type
			this.id = id
		}		
	}
	static final def XML_SLURPER = new XmlSlurper()
	static final def ALLOWED_ITEM_TYPES = [
			'CONTAINER_1',
			'CONTAINER_2',
			'CONTAINER_3',
			'ELEMENT_1',
			'ELEMENT_2',
			'ELEMENT_3',
		]

	def remoteSystem = null

	DataDownloader(remoteSystem) {
		
		this.remoteSystem = remoteSystem
	}
	
	def fetch(itemType, itemId, isFetchSubItems) {
		
		if (! (itemType in ALLOWED_ITEM_TYPES)) {
			return
		}
		def rootItem = remoteSystem.findItem(itemType, itemId)
						
		if (isFetchSubItems) {
			def subItemsRefs = findSubItemsRefsIn(rootItem)
			
			for (subItemRef in subItemsRefs) {
				fetch(subItemRef.type, subItemRef.id, isFetchSubItems)
			}
		}
	}
	
	def findSubItemsRefsIn(item) {

		println(item)

		def definition = XML_SLURPER.parseText(item)
		def subItemsRefs = definition.'**'.findAll { node ->
				node.name() == 'item-ref'
			}.collect { node ->
				return new Ref(node.@type, node.@id)
			}

		return subItemsRefs
	}
}
