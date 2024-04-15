package local

public class MapStructureBuilder {

	def mapStructure	= [:]
	def mapAliasIndex	= [:]
	def currentKey		= null
	def currentValue	= null

	MapStructureBuilder() {
		this.setCurrent('/', this.mapStructure)
	}

	def setCurrent(key, value) {
		this.currentKey			= key
		this.currentValue		= value
		this.mapAliasIndex['.']	= this.mapAliasIndex[this.currentKey] = this.currentValue

		return this
	}

	def at(alias) {
		def value = this.mapAliasIndex[alias]
		
		if (value == null) {
			throw new RuntimeException("[at()] Alias not found: (${alias})")
		}

		if (alias != '.') {
			this.setCurrent(alias, value)
		}

		return this
	}

	def getKey(key) {
		if (! (this.currentValue instanceof Map)) {
			throw new RuntimeException("[getKey()] Can't get key from element: (${key}) <- (${this.currentValue})")
		}
		def value = this.currentValue[key]
	
		if (value == null) {
			this.currentValue[key] = value = [:]
		}
		this.setCurrent(key, value)

		return this
	}

	def setKey(key, value) {
		return this.setKeyAs(key, value, key)
	}

	def setKeyAs(key, value, alias) {
		def isAliased = alias != key
		
		if (! (this.currentValue instanceof Map)) {
			def suffix = isAliased ? 'As' : ''
			throw new RuntimeException("[setKey${suffix}()] Can't add key to element: (${key}) -> (${this.currentValue})")
		}
		this.currentValue[key] = this.mapAliasIndex[key] = value
		
		if (isAliased) {
			this.mapAliasIndex[alias] = value
		}

		return this
	}

	def addElement() {
		if (! (this.currentValue instanceof List)) {
			throw new RuntimeException("[addElement()] Can't add element to: (${this.currentValue})")
		}
		def value = [:]
		this.currentValue << value

		this.currentValue = value

		return this
	}
	
	String toString() {
		def s = "=============\n"
		s += "MAP STRUCTURE\n"
		s += "=============\n"
		s += this.mapStructure + "\n"
		s += "\n"
		s += "===========\n"
		s += "ALIAS INDEX\n"
		s += "===========\n"
		
		mapAliasIndex.each { key, value ->
			s += "${key}\t->\t${value}\n"
		}
		
		return s
	}
}
