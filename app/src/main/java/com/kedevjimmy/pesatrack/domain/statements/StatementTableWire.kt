package com.kedevjimmy.pesatrack.domain.statements

/**
 * Local, versioned length-prefixed table transport. No delimiter escaping,
 * JSON dependency, CSV serialization or customer metadata. Physical evidence
 * is verified in the isolated service; only ordered cells cross this boundary.
 */
object StatementTableWire {
    private const val PREFIX="PESA_TABLE_1:"
    private const val MAX_CHARS=500_000
    fun encode(tables:List<StatementTablePage>):String {
        require(tables.size in 1..5_000 && tables.sumOf {it.rows.size} in 1..5_000)
        val out=StringBuilder(PREFIX)
        fun field(value:String) {
            require(value.length<=1200)
            out.append(value.length).append(':').append(value)
            require(out.length<=MAX_CHARS)
        }
        fun number(n:Int)=field(n.toString())
        number(tables.size)
        for(table in tables) {
            require(table.pageNumber in 1..160 && table.headers.size in 3..40 && table.rows.isNotEmpty())
            require(table.tableNumber in 1..5_000)
            number(table.pageNumber);number(table.tableNumber);number(table.headers.size);number(table.rows.size)
            table.headers.forEach(::field)
            table.rows.forEach {row->require(row.size==table.headers.size);row.forEach(::field)}
        }
        return out.toString()
    }
    fun decode(encoded:String):List<StatementTablePage> {
        require(encoded.length<=MAX_CHARS && encoded.startsWith(PREFIX))
        var offset=PREFIX.length
        fun field():String {
            val end=encoded.indexOf(':',offset)
            require(end in offset..minOf(offset+4,encoded.lastIndex))
            val digits=encoded.substring(offset,end)
            require(digits.isNotEmpty() && digits.all {it in '0'..'9'})
            val length=digits.toInt()
            require(length<=1200 && end+1+length<=encoded.length)
            offset=end+1+length
            return encoded.substring(end+1,offset)
        }
        fun number():Int {
            val text=field()
            require(text.length in 1..4 && text.all {it in '0'..'9'})
            return text.toInt()
        }
        val count=number();require(count in 1..5_000)
        var rows=0
        var previousPage=0
        var previousTable=0
        val tables=List(count) {
            val page=number();val tableNumber=number();val columns=number();val n=number()
            rows+=n
            require(page in 1..160 && page>=previousPage && columns in 3..40 && n in 1..5_000 && rows<=5_000)
            require(tableNumber in 1..5_000 && (page>previousPage || tableNumber>previousTable))
            previousPage=page
            previousTable=tableNumber
            val headers=List(columns){field()}
            require(headers.all {it.isNotBlank()})
            StatementTablePage(page,headers,List(n){List(columns){field()}},tableNumber=tableNumber)
        }
        require(offset==encoded.length)
        return tables
    }
}
