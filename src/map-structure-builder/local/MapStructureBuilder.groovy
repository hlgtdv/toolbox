package local

public class MapStructureBuilder extends StructureBuilder {

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
			throw new RuntimeException("Alias not found: (${alias})")
		}

		this.setCurrent(alias, value)

		return this
	}

	def getEntry(key) {
		return getEntryAs(key, key)
	}

	def getEntryAs(key, alias, defaultValue=[:]) {
		def isAliased = alias != key

		if (! (this.currentValue instanceof Map)) {
			throw new RuntimeException("Can't get key from element: (${key}) <- (${this.currentValue})")
		}
		def value = this.currentValue[key]
	
		if (value == null) {
			this.currentValue[key] = value = defaultValue
		}
		this.setCurrent(key, value)

		if (isAliased) {
			this.mapAliasIndex[alias] = value
		}

		return this
	}

	def getListEntry(key) {
		return getListEntryAs(key, key)
	}

	def getListEntryAs(key, alias) {
		return getEntryAs(key, alias, [])
	}

	def getElementAt(index) {
		if (! (this.currentValue instanceof List)) {
			throw new RuntimeException("Can't get element at ${index} from: (${this.currentValue})")
		}
		def value = this.currentValue[index]

		this.setCurrent("${this.currentKey}[${index}]", value)

		return this
	}

	def getLastElement() {
		if (! (this.currentValue instanceof List)) {
			throw new RuntimeException("Can't get last element from: (${this.currentValue})")
		}
		getElementAt(this.currentValue.size() - 1)

		return this
	}	
	
	def addElement() {
		if (! (this.currentValue instanceof List)) {
			throw new RuntimeException("Can't add element to: (${this.currentValue})")
		}
		def value = [:]
		
		this.currentValue << value
		this.currentValue = value

		return this
	}

	def setEntry(key, value) {
		return this.setEntryAs(key, value, key)
	}

	def setEntryAs(key, value, alias) {
		def isAliased = alias != key
		
		if (! (this.currentValue instanceof Map)) {
			throw new RuntimeException("Can't add key to element: (${key}) -> (${this.currentValue})")
		}
		this.currentValue[key] = this.mapAliasIndex[key] = value
		
		if (isAliased) {
			this.mapAliasIndex[alias] = value
		}

		return this
	}

	String display() {
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
		s += "\n"
		s += "=======\n"
		s += "CURRENT\n"
		s += "=======\n"
		s += "${this.currentKey}\t->\t${this.currentValue}\n"
		s += "\n"

		println s
	}
}
