// Important variables
var col = 0
var row = 0
var rowWhite = 1
var rowBlack = 1
var indexWhite = 1
var indexBlack = 1
var boardContent = "<table  class='chessboard'>"
var activeFieldID = 0
var finalPosition = 0
var thisPiece = ""
var player = "Weiss";
var opponent = ""
var id = ""

//DRY
const blackDeathList = document.getElementById("blackDeathList")
const chessboard = document.getElementById("chessboard")
const whiteDeathList = document.getElementById("whiteDeathList")

//Pieces
const whitePawn = '<img src="Icons/white_pawn.png">'
const whiteRook = '<img src="Icons/white_rook.png">'
const whiteBishop = '<img src="Icons/white_bishop.png">'
const whiteKnight = '<img src="Icons/white_knight.png">'
const whiteQueen = '<img src="Icons/white_queen.png">'
const whiteKing = '<img src="Icons/white_king.png">'
const blackPawn = '<img src="Icons/black_pawn.png">'
const blackRook = '<img src="Icons/black_rook.png">'
const blackBishop = '<img src="Icons/black_bishop.png">'
const blackKnight = '<img src="Icons/black_knight.png">'
const blackQueen = '<img src="Icons/black_queen.png">'
const blackKing = '<img src="Icons/black_king.png">'

//Important constants
const occupancy = []
const color = []
const pieces = []
const deadWhite = []
const deadBlack = []
const deadWhitePawns = []
const deadBlackPawns = []
const white = "Weiss"
const black = "Schwarz"

//Create the chessboard
function CreateChessboard() {
    //Reset variables
    activeFieldID = 0
    boardContent = "<table  class='chessboard'>"
    row = 0
    col = 0
    rowBlack = 1
    rowWhite = 1
    player = white
    let id = ""

    //Reset Deathlist
    for (let x = 1; x <= 16;) {
        id = "white" + x
        document.getElementById(id).innerHTML = ""
        deadWhite[x] = ""
        deadWhitePawns[x] = ""
        id = "black" + x
        document.getElementById(id).innerHTML = ""
        deadBlack[x] = ""
        deadBlackPawns[x] = ""
        x++
    }

    //Add pieces
    for (let x = 0; x < 64;) {
        pieces[x++] = ""
    }

    pieces.splice(0, 16,
        blackRook,
        blackKnight,
        blackBishop,
        blackQueen,
        blackKing,
        blackBishop,
        blackKnight,
        blackRook,
        blackPawn,
        blackPawn,
        blackPawn,
        blackPawn,
        blackPawn,
        blackPawn,
        blackPawn,
        blackPawn)

    pieces.splice(48, 16,
        whitePawn,
        whitePawn,
        whitePawn,
        whitePawn,
        whitePawn,
        whitePawn,
        whitePawn,
        whitePawn,
        whiteRook,
        whiteKnight,
        whiteBishop,
        whiteQueen,
        whiteKing,
        whiteBishop,
        whiteKnight,
        whiteRook)


    //Add colors
    for (let x = 0; x < 64;) {
        color[x++] = ""
    }

    color.splice(0, 16,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black,
        black)

    color.splice(48, 16,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white,
        white)

    //Create table
    while (row < 8) {
        boardContent += "<tr>"
        while (col < 8) {
            //Set the occupancy
            if (pieces[activeFieldID] == "") {
                occupancy[activeFieldID] = false
            }
            else {
                occupancy[activeFieldID] = true
            }
            //Adding a function to select which piece should move, the fieldID and the piece
            boardContent += "<td class='piece' onclick='move(" + activeFieldID + ")' id='" + activeFieldID + "'>" + pieces[activeFieldID++] + "</td>"
            col++
        }
        boardContent += "</tr>"
        row++
        col = 0
    }
    boardContent += "</table>"
    //Visualize the table
    chessboard.innerHTML = boardContent
}

//Choose which piece should move
function move(startPosition) {
    //Checking if there is a piece
    if (occupancy[startPosition] == false) {
        alert("Hier steht keine Figur.")
    } else {
        //Reset Variables
        boardContent = "<table  class='chessboard' id='chessboard'>"
        row = 0
        col = 0
        activeFieldID = 0

        //Create a new Table
        while (row < 8) {
            boardContent += "<tr>"
            while (col < 8) {
                //Adding a function to select where the active piece should move to, the fieldID and the piece
                boardContent += "<td class='piece' onclick='toHere(" + activeFieldID + ")' id='" + activeFieldID + "'>" + pieces[activeFieldID++] + "</td>"
                col++
            }
            boardContent += "</tr>"
            row++
            col = 0
        }
        boardContent += "</table>"
        //Visualize the new table
        chessboard.innerHTML = boardContent
        //Modify the border of the chessboard
        chessboard.style = "border-right: outset 5px #2d2dd4; border-left: outset 5px #4f4ff6; border-bottom: outset 5px #2d2dd4; border-top: outset 5px #4f4ff6"
        activeFieldID = startPosition
    }
    let activeType = pieces[startPosition]
    //Check if the active piece is a white pawn
    if (player == white) {
        if (activeType == whitePawn) {
            //Check if there is a black piece
            finalPosition = startPosition - 9
            if (color[finalPosition] == black && document.getElementById(finalPosition)) {
                //Check if the target field is one row further
                if (Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8)) {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition - 7
            if (color[finalPosition] == black && document.getElementById(finalPosition)) {
                if (Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8)) {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition - 16
            if (startPosition > 47 && document.getElementById(finalPosition)) {
                if (color[finalPosition] == "" && color[startPosition - 8] == "") {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition - 8
            if (color[finalPosition] == "" && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        } else if (activeType == whiteRook) {
            if (color[startPosition - 8] == "" && document.getElementById(startPosition - 8)) {
                if (color[startPosition - 16] == "" && document.getElementById(startPosition - 16)) {
                    if (color[startPosition - 24] == "" && document.getElementById(startPosition - 24)) {
                        if (color[startPosition - 32] == "" && document.getElementById(startPosition - 32)) {
                            if (color[startPosition - 40] == "" && document.getElementById(startPosition - 40)) {
                                if (color[startPosition - 48] == "" && document.getElementById(startPosition - 48)) {
                                    if (color[startPosition - 56] == "" && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition - 56] == black && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 48] == black && document.getElementById(startPosition - 48)) {
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition - 40] == black && document.getElementById(startPosition - 40)) {
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 32] == black && document.getElementById(startPosition - 32)) {
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition - 24] == black && document.getElementById(startPosition - 24)) {
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 16] == black && document.getElementById(startPosition - 16)) {
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition - 8] == black && document.getElementById(startPosition - 8)) {
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (color[startPosition + 8] == "" && document.getElementById(startPosition + 8)) {
                if (color[startPosition + 16] == "" && document.getElementById(startPosition + 16)) {
                    if (color[startPosition + 24] == "" && document.getElementById(startPosition + 24)) {
                        if (color[startPosition + 32] == "" && document.getElementById(startPosition + 32)) {
                            if (color[startPosition + 40] == "" && document.getElementById(startPosition + 40)) {
                                if (color[startPosition + 48] == "" && document.getElementById(startPosition + 48)) {
                                    if (color[startPosition + 56] == "" && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition + 56] == black && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 48] == black && document.getElementById(startPosition + 48)) {
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition + 40] == black && document.getElementById(startPosition + 40)) {
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 32] == black && document.getElementById(startPosition + 32)) {
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition + 24] == black && document.getElementById(startPosition + 24)) {
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 16] == black && document.getElementById(startPosition + 16)) {
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition + 8] == black && document.getElementById(startPosition + 8)) {
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 1) / 8)) {
                if (color[startPosition + 1] == "" && document.getElementById(startPosition + 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 2) / 8)) {
                        if (color[startPosition + 2] == "" && document.getElementById(startPosition + 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 3) / 8)) {
                                if (color[startPosition + 3] == "" && document.getElementById(startPosition + 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 4) / 8)) {
                                        if (color[startPosition + 4] == "" && document.getElementById(startPosition + 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 5) / 8)) {
                                                if (color[startPosition + 5] == "" && document.getElementById(startPosition + 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 6) / 8)) {
                                                        if (color[startPosition + 6] == "" && document.getElementById(startPosition + 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 7) / 8)) {
                                                                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 7] == black && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 6] == black && document.getElementById(startPosition + 6)) {
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 5] == black && document.getElementById(startPosition + 5)) {
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 4] == black && document.getElementById(startPosition + 4)) {
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 3] == black && document.getElementById(startPosition + 3)) {
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 2] == black && document.getElementById(startPosition + 2)) {
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 1] == black && document.getElementById(startPosition + 1)) {
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 1) / 8)) {
                if (color[startPosition - 1] == "" && document.getElementById(startPosition - 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 2) / 8)) {
                        if (color[startPosition - 2] == "" && document.getElementById(startPosition - 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 3) / 8)) {
                                if (color[startPosition - 3] == "" && document.getElementById(startPosition - 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 4) / 8)) {
                                        if (color[startPosition - 4] == "" && document.getElementById(startPosition - 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 5) / 8)) {
                                                if (color[startPosition - 5] == "" && document.getElementById(startPosition - 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 6) / 8)) {
                                                        if (color[startPosition - 6] == "" && document.getElementById(startPosition - 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 7) / 8)) {
                                                                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 7] == black && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 6] == black && document.getElementById(startPosition - 6)) {
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 5] == black && document.getElementById(startPosition - 5)) {
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 4] == black && document.getElementById(startPosition - 4)) {
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 3] == black && document.getElementById(startPosition - 3)) {
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 2] == black && document.getElementById(startPosition - 2)) {
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 1] == black && document.getElementById(startPosition - 1)) {
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

        } else if (activeType == whiteKnight) {
            finalPosition = startPosition - 15
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(startPosition / 8 - 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 17
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(startPosition / 8 - 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 6
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 - 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 10
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 - 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 15
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 17
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 6
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 10
            if (color[finalPosition] != white && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        } else if (activeType == whiteBishop) {
            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 7) / 8)) {
                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 14) / 8)) {
                        if (color[startPosition - 14] == "" && document.getElementById(startPosition - 14)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 21) / 8)) {
                                if (color[startPosition - 21] == "" && document.getElementById(startPosition - 21)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 28) / 8)) {
                                        if (color[startPosition - 28] == "" && document.getElementById(startPosition - 28)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 35) / 8)) {
                                                if (color[startPosition - 35] == "" && document.getElementById(startPosition - 35)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 49] == black && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 42] == black && document.getElementById(startPosition - 42)) {
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 35] == black && document.getElementById(startPosition - 35)) {
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 28] == black && document.getElementById(startPosition - 28)) {
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 21] == black && document.getElementById(startPosition - 21)) {
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 14] == black && document.getElementById(startPosition - 14)) {
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 7] == black && document.getElementById(startPosition - 7)) {
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 7) / 8)) {
                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 14) / 8)) {
                        if (color[startPosition + 14] == "" && document.getElementById(startPosition + 14)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 21) / 8)) {
                                if (color[startPosition + 21] == "" && document.getElementById(startPosition + 21)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 28) / 8)) {
                                        if (color[startPosition + 28] == "" && document.getElementById(startPosition + 28)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 35) / 8)) {
                                                if (color[startPosition + 35] == "" && document.getElementById(startPosition + 35)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 49] == black && document.getElementById(startPosition + 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 42] == black && document.getElementById(startPosition + 42)) {
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 35] == black && document.getElementById(startPosition + 35)) {
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 28] == black && document.getElementById(startPosition + 28)) {
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 21] == black && document.getElementById(startPosition + 21)) {
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 14] == black && document.getElementById(startPosition + 14)) {
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 7] == black && document.getElementById(startPosition + 7)) {
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 9) / 8)) {
                if (color[startPosition - 9] == "" && document.getElementById(startPosition - 9)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 18) / 8)) {
                        if (color[startPosition - 18] == "" && document.getElementById(startPosition - 18)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 27) / 8)) {
                                if (color[startPosition - 27] == "" && document.getElementById(startPosition - 27)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 36) / 8)) {
                                        if (color[startPosition - 36] == "" && document.getElementById(startPosition - 36)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 45) / 8)) {
                                                if (color[startPosition - 45] == "" && document.getElementById(startPosition - 45)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 63] == black && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 54] == black && document.getElementById(startPosition - 54)) {
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 45] == black && document.getElementById(startPosition - 45)) {
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 36] == black && document.getElementById(startPosition - 36)) {
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 27] == black && document.getElementById(startPosition - 27)) {
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 18] == black && document.getElementById(startPosition - 18)) {
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 9] == black && document.getElementById(startPosition - 9)) {
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 9) / 8)) {
                if (color[startPosition + 9] == "" && document.getElementById(startPosition + 9)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 18) / 8)) {
                        if (color[startPosition + 18] == "" && document.getElementById(startPosition + 18)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 27) / 8)) {
                                if (color[startPosition + 27] == "" && document.getElementById(startPosition + 27)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 36) / 8)) {
                                        if (color[startPosition + 36] == "" && document.getElementById(startPosition + 36)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 45) / 8)) {
                                                if (color[startPosition + 45] == "" && document.getElementById(startPosition + 54)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 63] == black && document.getElementById(startPosition + 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 54] == black && document.getElementById(startPosition + 54)) {
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 45] == black && document.getElementById(startPosition + 45)) {
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 36] == black && document.getElementById(startPosition + 36)) {
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 27] == black && document.getElementById(startPosition + 27)) {
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 18] == black && document.getElementById(startPosition + 18)) {
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 9] == black && document.getElementById(startPosition + 9)) {
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
        } else if (activeType == whiteQueen) {
            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 7) / 8)) {
                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 14) / 8)) {
                        if (color[startPosition - 14] == "" && document.getElementById(startPosition - 14)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 21) / 8)) {
                                if (color[startPosition - 21] == "" && document.getElementById(startPosition - 21)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 28) / 8)) {
                                        if (color[startPosition - 28] == "" && document.getElementById(startPosition - 28)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 35) / 8)) {
                                                if (color[startPosition - 35] == "" && document.getElementById(startPosition - 35)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 49] == black && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 42] == black && document.getElementById(startPosition - 42)) {
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 35] == black && document.getElementById(startPosition - 35)) {
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 28] == black && document.getElementById(startPosition - 28)) {
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 21] == black && document.getElementById(startPosition - 21)) {
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 14] == black && document.getElementById(startPosition - 14)) {
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 7] == black && document.getElementById(startPosition - 7)) {
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 7) / 8)) {
                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 14) / 8)) {
                        if (color[startPosition + 14] == "" && document.getElementById(startPosition + 14)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 21) / 8)) {
                                if (color[startPosition + 21] == "" && document.getElementById(startPosition + 21)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 28) / 8)) {
                                        if (color[startPosition + 28] == "" && document.getElementById(startPosition + 28)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 35) / 8)) {
                                                if (color[startPosition + 35] == "" && document.getElementById(startPosition + 35)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 49] == black && document.getElementById(startPosition + 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 42] == black && document.getElementById(startPosition + 42)) {
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 35] == black && document.getElementById(startPosition + 35)) {
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 28] == black && document.getElementById(startPosition + 28)) {
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 21] == black && document.getElementById(startPosition + 21)) {
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 14] == black && document.getElementById(startPosition + 14)) {
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 7] == black && document.getElementById(startPosition + 7)) {
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 9) / 8)) {
                if (color[startPosition - 9] == "" && document.getElementById(startPosition - 9)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 18) / 8)) {
                        if (color[startPosition - 18] == "" && document.getElementById(startPosition - 18)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 27) / 8)) {
                                if (color[startPosition - 27] == "" && document.getElementById(startPosition - 27)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 36) / 8)) {
                                        if (color[startPosition - 36] == "" && document.getElementById(startPosition - 36)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 45) / 8)) {
                                                if (color[startPosition - 45] == "" && document.getElementById(startPosition - 45)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 63] == black && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 54] == black && document.getElementById(startPosition - 54)) {
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 45] == black && document.getElementById(startPosition - 45)) {
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 36] == black && document.getElementById(startPosition - 36)) {
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 27] == black && document.getElementById(startPosition - 27)) {
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 18] == black && document.getElementById(startPosition - 18)) {
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 9] == black && document.getElementById(startPosition - 9)) {
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 9) / 8)) {
                if (color[startPosition + 9] == "" && document.getElementById(startPosition + 9)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 18) / 8)) {
                        if (color[startPosition + 18] == "" && document.getElementById(startPosition + 18)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 27) / 8)) {
                                if (color[startPosition + 27] == "" && document.getElementById(startPosition + 27)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 36) / 8)) {
                                        if (color[startPosition + 36] == "" && document.getElementById(startPosition + 36)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 45) / 8)) {
                                                if (color[startPosition + 45] == "" && document.getElementById(startPosition + 54)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 63] == black && document.getElementById(startPosition + 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 54] == black && document.getElementById(startPosition + 54)) {
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 45] == black && document.getElementById(startPosition + 45)) {
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 36] == black && document.getElementById(startPosition + 36)) {
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 27] == black && document.getElementById(startPosition + 27)) {
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 18] == black && document.getElementById(startPosition + 18)) {
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 9] == black && document.getElementById(startPosition + 9)) {
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }


            if (color[startPosition - 8] == "" && document.getElementById(startPosition - 8)) {
                if (color[startPosition - 16] == "" && document.getElementById(startPosition - 16)) {
                    if (color[startPosition - 24] == "" && document.getElementById(startPosition - 24)) {
                        if (color[startPosition - 32] == "" && document.getElementById(startPosition - 32)) {
                            if (color[startPosition - 40] == "" && document.getElementById(startPosition - 40)) {
                                if (color[startPosition - 48] == "" && document.getElementById(startPosition - 48)) {
                                    if (color[startPosition - 56] == "" && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition - 56] == black && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 48] == black && document.getElementById(startPosition - 48)) {
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition - 40] == black && document.getElementById(startPosition - 40)) {
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 32] == black && document.getElementById(startPosition - 32)) {
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition - 24] == black && document.getElementById(startPosition - 24)) {
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 16] == black && document.getElementById(startPosition - 16)) {
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition - 8] == black && document.getElementById(startPosition - 8)) {
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (color[startPosition + 8] == "" && document.getElementById(startPosition + 8)) {
                if (color[startPosition + 16] == "" && document.getElementById(startPosition + 16)) {
                    if (color[startPosition + 24] == "" && document.getElementById(startPosition + 24)) {
                        if (color[startPosition + 32] == "" && document.getElementById(startPosition + 32)) {
                            if (color[startPosition + 40] == "" && document.getElementById(startPosition + 40)) {
                                if (color[startPosition + 48] == "" && document.getElementById(startPosition + 48)) {
                                    if (color[startPosition + 56] == "" && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition + 56] == black && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 48] == black && document.getElementById(startPosition + 48)) {
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition + 40] == black && document.getElementById(startPosition + 40)) {
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 32] == black && document.getElementById(startPosition + 32)) {
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition + 24] == black && document.getElementById(startPosition + 24)) {
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 16] == black && document.getElementById(startPosition + 16)) {
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition + 8] == black && document.getElementById(startPosition + 8)) {
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 1) / 8)) {
                if (color[startPosition + 1] == "" && document.getElementById(startPosition + 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 2) / 8)) {
                        if (color[startPosition + 2] == "" && document.getElementById(startPosition + 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 3) / 8)) {
                                if (color[startPosition + 3] == "" && document.getElementById(startPosition + 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 4) / 8)) {
                                        if (color[startPosition + 4] == "" && document.getElementById(startPosition + 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 5) / 8)) {
                                                if (color[startPosition + 5] == "" && document.getElementById(startPosition + 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 6) / 8)) {
                                                        if (color[startPosition + 6] == "" && document.getElementById(startPosition + 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 7) / 8)) {
                                                                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 7] == black && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 6] == black && document.getElementById(startPosition + 6)) {
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 5] == black && document.getElementById(startPosition + 5)) {
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 4] == black && document.getElementById(startPosition + 4)) {
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 3] == black && document.getElementById(startPosition + 3)) {
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 2] == black && document.getElementById(startPosition + 2)) {
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 1] == black && document.getElementById(startPosition + 1)) {
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 1) / 8)) {
                if (color[startPosition - 1] == "" && document.getElementById(startPosition - 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 2) / 8)) {
                        if (color[startPosition - 2] == "" && document.getElementById(startPosition - 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 3) / 8)) {
                                if (color[startPosition - 3] == "" && document.getElementById(startPosition - 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 4) / 8)) {
                                        if (color[startPosition - 4] == "" && document.getElementById(startPosition - 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 5) / 8)) {
                                                if (color[startPosition - 5] == "" && document.getElementById(startPosition - 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 6) / 8)) {
                                                        if (color[startPosition - 6] == "" && document.getElementById(startPosition - 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 7) / 8)) {
                                                                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 7] == black && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 6] == black && document.getElementById(startPosition - 6)) {
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 5] == black && document.getElementById(startPosition - 5)) {
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 4] == black && document.getElementById(startPosition - 4)) {
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 3] == black && document.getElementById(startPosition - 3)) {
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 2] == black && document.getElementById(startPosition - 2)) {
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 1] == black && document.getElementById(startPosition - 1)) {
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
        } else if (activeType == whiteKing) {
            finalPosition = startPosition - 1
            if (color[finalPosition] != white && Math.floor(startPosition / 8) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 1
            if (color[finalPosition] != white && Math.floor(startPosition / 8) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 8
            if (color[finalPosition] != white && Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 8
            if (color[finalPosition] != white && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 7
            if (color[finalPosition] != white && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 9
            if (color[finalPosition] != white && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 7
            if (color[finalPosition] != white && Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 9
            if (color[finalPosition] != white && Math.floor(activeFieldID / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        }
    } else if (player == black) {
        if (activeType == blackPawn) {
            //Check if there is a white piece
            finalPosition = startPosition + 9
            if (color[finalPosition] == white && document.getElementById(finalPosition)) {
                //Check if the target field is one row further
                if (Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8)) {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition + 7
            if (color[finalPosition] == white && document.getElementById(finalPosition)) {
                if (Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8)) {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition + 16
            if (startPosition < 16 && document.getElementById(finalPosition)) {
                if (color[finalPosition] == "" && color[startPosition + 8] == "") {
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
            finalPosition = startPosition + 8
            if (color[finalPosition] == "" && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        } else if (activeType == blackRook) {
            if (color[startPosition - 8] == "" && document.getElementById(startPosition - 8)) {
                if (color[startPosition - 16] == "" && document.getElementById(startPosition - 16)) {
                    if (color[startPosition - 24] == "" && document.getElementById(startPosition - 24)) {
                        if (color[startPosition - 32] == "" && document.getElementById(startPosition - 32)) {
                            if (color[startPosition - 40] == "" && document.getElementById(startPosition - 40)) {
                                if (color[startPosition - 48] == "" && document.getElementById(startPosition - 48)) {
                                    if (color[startPosition - 56] == "" && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition - 56] == white && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 48] == white && document.getElementById(startPosition - 48)) {
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition - 40] == white && document.getElementById(startPosition - 40)) {
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 32] == white && document.getElementById(startPosition - 32)) {
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition - 24] == white && document.getElementById(startPosition - 24)) {
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 16] == white && document.getElementById(startPosition - 16)) {
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition - 8] == white && document.getElementById(startPosition - 8)) {
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (color[startPosition + 8] == "" && document.getElementById(startPosition + 8)) {
                if (color[startPosition + 16] == "" && document.getElementById(startPosition + 16)) {
                    if (color[startPosition + 24] == "" && document.getElementById(startPosition + 24)) {
                        if (color[startPosition + 32] == "" && document.getElementById(startPosition + 32)) {
                            if (color[startPosition + 40] == "" && document.getElementById(startPosition + 40)) {
                                if (color[startPosition + 48] == "" && document.getElementById(startPosition + 48)) {
                                    if (color[startPosition + 56] == "" && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition + 56] == white && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 48] == white && document.getElementById(startPosition + 48)) {
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition + 40] == white && document.getElementById(startPosition + 40)) {
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 32] == white && document.getElementById(startPosition + 32)) {
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition + 24] == white && document.getElementById(startPosition + 24)) {
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 16] == white && document.getElementById(startPosition + 16)) {
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition + 8] == white && document.getElementById(startPosition + 8)) {
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 1) / 8)) {
                if (color[startPosition + 1] == "" && document.getElementById(startPosition + 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 2) / 8)) {
                        if (color[startPosition + 2] == "" && document.getElementById(startPosition + 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 3) / 8)) {
                                if (color[startPosition + 3] == "" && document.getElementById(startPosition + 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 4) / 8)) {
                                        if (color[startPosition + 4] == "" && document.getElementById(startPosition + 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 5) / 8)) {
                                                if (color[startPosition + 5] == "" && document.getElementById(startPosition + 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 6) / 8)) {
                                                        if (color[startPosition + 6] == "" && document.getElementById(startPosition + 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 7) / 8)) {
                                                                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 7] == white && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 6] == white && document.getElementById(startPosition + 6)) {
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 5] == white && document.getElementById(startPosition + 5)) {
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 4] == white && document.getElementById(startPosition + 4)) {
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 3] == white && document.getElementById(startPosition + 3)) {
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 2] == white && document.getElementById(startPosition + 2)) {
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 1] == white && document.getElementById(startPosition + 1)) {
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 1) / 8)) {
                if (color[startPosition - 1] == "" && document.getElementById(startPosition - 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 2) / 8)) {
                        if (color[startPosition - 2] == "" && document.getElementById(startPosition - 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 3) / 8)) {
                                if (color[startPosition - 3] == "" && document.getElementById(startPosition - 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 4) / 8)) {
                                        if (color[startPosition - 4] == "" && document.getElementById(startPosition - 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 5) / 8)) {
                                                if (color[startPosition - 5] == "" && document.getElementById(startPosition - 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 6) / 8)) {
                                                        if (color[startPosition - 6] == "" && document.getElementById(startPosition - 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 7) / 8)) {
                                                                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 7] == white && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 6] == white && document.getElementById(startPosition - 6)) {
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 5] == white && document.getElementById(startPosition - 5)) {
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 4] == white && document.getElementById(startPosition - 4)) {
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 3] == white && document.getElementById(startPosition - 3)) {
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 2] == white && document.getElementById(startPosition - 2)) {
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 1] == white && document.getElementById(startPosition - 1)) {
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
        } else if (activeType == blackKnight) {
            finalPosition = startPosition - 15
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(startPosition / 8 - 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 17
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(startPosition / 8 - 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 6
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 - 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 10
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 - 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 15
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 17
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 2) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 6
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 10
            if (color[finalPosition] != black && Math.floor(finalPosition / 8) == Math.floor(activeFieldID / 8 + 1) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        } else if (activeType == blackBishop) {
            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 7) / 8)) {
                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 14) / 8)) {
                        if (color[startPosition - 14] == "" && document.getElementById(startPosition - 14)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 21) / 8)) {
                                if (color[startPosition - 21] == "" && document.getElementById(startPosition - 21)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 28) / 8)) {
                                        if (color[startPosition - 28] == "" && document.getElementById(startPosition - 28)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 35) / 8)) {
                                                if (color[startPosition - 35] == "" && document.getElementById(startPosition - 35)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 49] == white && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 42] == white && document.getElementById(startPosition - 42)) {
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 35] == white && document.getElementById(startPosition - 35)) {
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 28] == white && document.getElementById(startPosition - 28)) {
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 21] == white && document.getElementById(startPosition - 21)) {
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 14] == white && document.getElementById(startPosition - 14)) {
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 7] == white && document.getElementById(startPosition - 7)) {
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 7) / 8)) {
                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 14) / 8)) {
                        if (color[startPosition + 14] == "" && document.getElementById(startPosition + 14)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 21) / 8)) {
                                if (color[startPosition + 21] == "" && document.getElementById(startPosition + 21)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 28) / 8)) {
                                        if (color[startPosition + 28] == "" && document.getElementById(startPosition + 28)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 35) / 8)) {
                                                if (color[startPosition + 35] == "" && document.getElementById(startPosition + 35)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 49] == white && document.getElementById(startPosition + 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 42] == white && document.getElementById(startPosition + 42)) {
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 35] == white && document.getElementById(startPosition + 35)) {
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 28] == white && document.getElementById(startPosition + 28)) {
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 21] == white && document.getElementById(startPosition + 21)) {
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 14] == white && document.getElementById(startPosition + 14)) {
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 7] == white && document.getElementById(startPosition + 7)) {
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 9) / 8)) {
                if (color[startPosition - 9] == "" && document.getElementById(startPosition - 9)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 18) / 8)) {
                        if (color[startPosition - 18] == "" && document.getElementById(startPosition - 18)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 27) / 8)) {
                                if (color[startPosition - 27] == "" && document.getElementById(startPosition - 27)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 36) / 8)) {
                                        if (color[startPosition - 36] == "" && document.getElementById(startPosition - 36)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 45) / 8)) {
                                                if (color[startPosition - 45] == "" && document.getElementById(startPosition - 45)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 63] == white && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 54] == white && document.getElementById(startPosition - 54)) {
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 45] == white && document.getElementById(startPosition - 45)) {
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 36] == white && document.getElementById(startPosition - 36)) {
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 27] == white && document.getElementById(startPosition - 27)) {
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 18] == white && document.getElementById(startPosition - 18)) {
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 9] == white && document.getElementById(startPosition - 9)) {
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 9) / 8)) {
                if (color[startPosition + 9] == "" && document.getElementById(startPosition + 9)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 18) / 8)) {
                        if (color[startPosition + 18] == "" && document.getElementById(startPosition + 18)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 27) / 8)) {
                                if (color[startPosition + 27] == "" && document.getElementById(startPosition + 27)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 36) / 8)) {
                                        if (color[startPosition + 36] == "" && document.getElementById(startPosition + 36)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 45) / 8)) {
                                                if (color[startPosition + 45] == "" && document.getElementById(startPosition + 54)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 63] == white && document.getElementById(startPosition + 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 54] == white && document.getElementById(startPosition + 54)) {
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 45] == white && document.getElementById(startPosition + 45)) {
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 36] == white && document.getElementById(startPosition + 36)) {
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 27] == white && document.getElementById(startPosition + 27)) {
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 18] == white && document.getElementById(startPosition + 18)) {
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 9] == white && document.getElementById(startPosition + 9)) {
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
        } else if (activeType == blackQueen) {
            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 7) / 8)) {
                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 14) / 8)) {
                        if (color[startPosition - 14] == "" && document.getElementById(startPosition - 14)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 21) / 8)) {
                                if (color[startPosition - 21] == "" && document.getElementById(startPosition - 21)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 28) / 8)) {
                                        if (color[startPosition - 28] == "" && document.getElementById(startPosition - 28)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 35) / 8)) {
                                                if (color[startPosition - 35] == "" && document.getElementById(startPosition - 35)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 49] == white && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition - 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 42] == white && document.getElementById(startPosition - 42)) {
                                                            finalPosition = startPosition - 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 35] == white && document.getElementById(startPosition - 35)) {
                                                    finalPosition = startPosition - 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 28] == white && document.getElementById(startPosition - 28)) {
                                            finalPosition = startPosition - 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 21] == white && document.getElementById(startPosition - 21)) {
                                    finalPosition = startPosition - 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 14] == white && document.getElementById(startPosition - 14)) {
                            finalPosition = startPosition - 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 7] == white && document.getElementById(startPosition - 7)) {
                    finalPosition = startPosition - 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 7) / 8)) {
                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 14) / 8)) {
                        if (color[startPosition + 14] == "" && document.getElementById(startPosition + 14)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 21) / 8)) {
                                if (color[startPosition + 21] == "" && document.getElementById(startPosition + 21)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 28) / 8)) {
                                        if (color[startPosition + 28] == "" && document.getElementById(startPosition + 28)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 35) / 8)) {
                                                if (color[startPosition + 35] == "" && document.getElementById(startPosition + 35)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 42) / 8)) {
                                                        if (color[startPosition - 42] == "" && document.getElementById(startPosition - 42)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 49) / 8)) {
                                                                if (color[startPosition - 49] == "" && document.getElementById(startPosition - 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 49] == white && document.getElementById(startPosition + 49)) {
                                                                    finalPosition = startPosition + 49
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 42] == white && document.getElementById(startPosition + 42)) {
                                                            finalPosition = startPosition + 42
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 35] == white && document.getElementById(startPosition + 35)) {
                                                    finalPosition = startPosition + 35
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 28] == white && document.getElementById(startPosition + 28)) {
                                            finalPosition = startPosition + 28
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 21] == white && document.getElementById(startPosition + 21)) {
                                    finalPosition = startPosition + 21
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 14] == white && document.getElementById(startPosition + 14)) {
                            finalPosition = startPosition + 14
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 7] == white && document.getElementById(startPosition + 7)) {
                    finalPosition = startPosition + 7
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 - 1) == Math.floor((startPosition - 9) / 8)) {
                if (color[startPosition - 9] == "" && document.getElementById(startPosition - 9)) {
                    if (Math.floor(startPosition / 8 - 2) == Math.floor((startPosition - 18) / 8)) {
                        if (color[startPosition - 18] == "" && document.getElementById(startPosition - 18)) {
                            if (Math.floor(startPosition / 8 - 3) == Math.floor((startPosition - 27) / 8)) {
                                if (color[startPosition - 27] == "" && document.getElementById(startPosition - 27)) {
                                    if (Math.floor(startPosition / 8 - 4) == Math.floor((startPosition - 36) / 8)) {
                                        if (color[startPosition - 36] == "" && document.getElementById(startPosition - 36)) {
                                            if (Math.floor(startPosition / 8 - 5) == Math.floor((startPosition - 45) / 8)) {
                                                if (color[startPosition - 45] == "" && document.getElementById(startPosition - 45)) {
                                                    if (Math.floor(startPosition / 8 - 6) == Math.floor((startPosition - 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 - 7) == Math.floor((startPosition - 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 63] == white && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition - 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 54] == white && document.getElementById(startPosition - 54)) {
                                                            finalPosition = startPosition - 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 45] == white && document.getElementById(startPosition - 45)) {
                                                    finalPosition = startPosition - 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 36] == white && document.getElementById(startPosition - 36)) {
                                            finalPosition = startPosition - 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 27] == white && document.getElementById(startPosition - 27)) {
                                    finalPosition = startPosition - 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 18] == white && document.getElementById(startPosition - 18)) {
                            finalPosition = startPosition - 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 9] == white && document.getElementById(startPosition - 9)) {
                    finalPosition = startPosition - 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8 + 1) == Math.floor((startPosition + 9) / 8)) {
                if (color[startPosition + 9] == "" && document.getElementById(startPosition + 9)) {
                    if (Math.floor(startPosition / 8 + 2) == Math.floor((startPosition + 18) / 8)) {
                        if (color[startPosition + 18] == "" && document.getElementById(startPosition + 18)) {
                            if (Math.floor(startPosition / 8 + 3) == Math.floor((startPosition + 27) / 8)) {
                                if (color[startPosition + 27] == "" && document.getElementById(startPosition + 27)) {
                                    if (Math.floor(startPosition / 8 + 4) == Math.floor((startPosition + 36) / 8)) {
                                        if (color[startPosition + 36] == "" && document.getElementById(startPosition + 36)) {
                                            if (Math.floor(startPosition / 8 + 5) == Math.floor((startPosition + 45) / 8)) {
                                                if (color[startPosition + 45] == "" && document.getElementById(startPosition + 54)) {
                                                    if (Math.floor(startPosition / 8 + 6) == Math.floor((startPosition + 54) / 8)) {
                                                        if (color[startPosition - 54] == "" && document.getElementById(startPosition - 54)) {
                                                            if (Math.floor(startPosition / 8 + 7) == Math.floor((startPosition + 63) / 8)) {
                                                                if (color[startPosition - 63] == "" && document.getElementById(startPosition - 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 63] == white && document.getElementById(startPosition + 63)) {
                                                                    finalPosition = startPosition + 63
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 54] == white && document.getElementById(startPosition + 54)) {
                                                            finalPosition = startPosition + 54
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 45] == white && document.getElementById(startPosition + 45)) {
                                                    finalPosition = startPosition + 45
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 36] == white && document.getElementById(startPosition + 36)) {
                                            finalPosition = startPosition + 36
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 27] == white && document.getElementById(startPosition + 27)) {
                                    finalPosition = startPosition + 27
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 18] == white && document.getElementById(startPosition + 18)) {
                            finalPosition = startPosition + 18
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 9] == white && document.getElementById(startPosition + 9)) {
                    finalPosition = startPosition + 9
                    document.getElementById(finalPosition).classList += "possible"
                }
            }


            if (color[startPosition - 8] == "" && document.getElementById(startPosition - 8)) {
                if (color[startPosition - 16] == "" && document.getElementById(startPosition - 16)) {
                    if (color[startPosition - 24] == "" && document.getElementById(startPosition - 24)) {
                        if (color[startPosition - 32] == "" && document.getElementById(startPosition - 32)) {
                            if (color[startPosition - 40] == "" && document.getElementById(startPosition - 40)) {
                                if (color[startPosition - 48] == "" && document.getElementById(startPosition - 48)) {
                                    if (color[startPosition - 56] == "" && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition - 56] == white && document.getElementById(startPosition - 56)) {
                                        finalPosition = startPosition - 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 48] == white && document.getElementById(startPosition - 48)) {
                                    finalPosition = startPosition - 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition - 40] == white && document.getElementById(startPosition - 40)) {
                                finalPosition = startPosition - 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 32] == white && document.getElementById(startPosition - 32)) {
                            finalPosition = startPosition - 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition - 24] == white && document.getElementById(startPosition - 24)) {
                        finalPosition = startPosition - 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 16] == white && document.getElementById(startPosition - 16)) {
                    finalPosition = startPosition - 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition - 8] == white && document.getElementById(startPosition - 8)) {
                finalPosition = startPosition - 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (color[startPosition + 8] == "" && document.getElementById(startPosition + 8)) {
                if (color[startPosition + 16] == "" && document.getElementById(startPosition + 16)) {
                    if (color[startPosition + 24] == "" && document.getElementById(startPosition + 24)) {
                        if (color[startPosition + 32] == "" && document.getElementById(startPosition + 32)) {
                            if (color[startPosition + 40] == "" && document.getElementById(startPosition + 40)) {
                                if (color[startPosition + 48] == "" && document.getElementById(startPosition + 48)) {
                                    if (color[startPosition + 56] == "" && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    } else if (color[startPosition + 56] == white && document.getElementById(startPosition + 56)) {
                                        finalPosition = startPosition + 56
                                        document.getElementById(finalPosition).classList += "possible"
                                    }
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 48] == white && document.getElementById(startPosition + 48)) {
                                    finalPosition = startPosition + 48
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            } else if (color[startPosition + 40] == white && document.getElementById(startPosition + 40)) {
                                finalPosition = startPosition + 40
                                document.getElementById(finalPosition).classList += "possible"
                            }
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 32] == white && document.getElementById(startPosition + 32)) {
                            finalPosition = startPosition + 32
                            document.getElementById(finalPosition).classList += "possible"
                        }
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    } else if (color[startPosition + 24] == white && document.getElementById(startPosition + 24)) {
                        finalPosition = startPosition + 24
                        document.getElementById(finalPosition).classList += "possible"
                    }
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 16] == white && document.getElementById(startPosition + 16)) {
                    finalPosition = startPosition + 16
                    document.getElementById(finalPosition).classList += "possible"
                }
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            } else if (color[startPosition + 8] == white && document.getElementById(startPosition + 8)) {
                finalPosition = startPosition + 8
                document.getElementById(finalPosition).classList += "possible"
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 1) / 8)) {
                if (color[startPosition + 1] == "" && document.getElementById(startPosition + 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 2) / 8)) {
                        if (color[startPosition + 2] == "" && document.getElementById(startPosition + 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 3) / 8)) {
                                if (color[startPosition + 3] == "" && document.getElementById(startPosition + 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 4) / 8)) {
                                        if (color[startPosition + 4] == "" && document.getElementById(startPosition + 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 5) / 8)) {
                                                if (color[startPosition + 5] == "" && document.getElementById(startPosition + 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition + 6) / 8)) {
                                                        if (color[startPosition + 6] == "" && document.getElementById(startPosition + 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition + 7) / 8)) {
                                                                if (color[startPosition + 7] == "" && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition + 7] == white && document.getElementById(startPosition + 7)) {
                                                                    finalPosition = startPosition + 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition + 6] == white && document.getElementById(startPosition + 6)) {
                                                            finalPosition = startPosition + 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition + 5] == white && document.getElementById(startPosition + 5)) {
                                                    finalPosition = startPosition + 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition + 4] == white && document.getElementById(startPosition + 4)) {
                                            finalPosition = startPosition + 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition + 3] == white && document.getElementById(startPosition + 3)) {
                                    finalPosition = startPosition + 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition + 2] == white && document.getElementById(startPosition + 2)) {
                            finalPosition = startPosition + 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition + 1] == white && document.getElementById(startPosition + 1)) {
                    finalPosition = startPosition + 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }

            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 1) / 8)) {
                if (color[startPosition - 1] == "" && document.getElementById(startPosition - 1)) {
                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 2) / 8)) {
                        if (color[startPosition - 2] == "" && document.getElementById(startPosition - 2)) {
                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 3) / 8)) {
                                if (color[startPosition - 3] == "" && document.getElementById(startPosition - 3)) {
                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 4) / 8)) {
                                        if (color[startPosition - 4] == "" && document.getElementById(startPosition - 4)) {
                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 5) / 8)) {
                                                if (color[startPosition - 5] == "" && document.getElementById(startPosition - 5)) {
                                                    if (Math.floor(startPosition / 8) == Math.floor((startPosition - 6) / 8)) {
                                                        if (color[startPosition - 6] == "" && document.getElementById(startPosition - 6)) {
                                                            if (Math.floor(startPosition / 8) == Math.floor((startPosition - 7) / 8)) {
                                                                if (color[startPosition - 7] == "" && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                } else if (color[startPosition - 7] == white && document.getElementById(startPosition - 7)) {
                                                                    finalPosition = startPosition - 7
                                                                    document.getElementById(finalPosition).classList += "possible"
                                                                }
                                                            }
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        } else if (color[startPosition - 6] == white && document.getElementById(startPosition - 6)) {
                                                            finalPosition = startPosition - 6
                                                            document.getElementById(finalPosition).classList += "possible"
                                                        }
                                                    }
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                } else if (color[startPosition - 5] == white && document.getElementById(startPosition - 5)) {
                                                    finalPosition = startPosition - 5
                                                    document.getElementById(finalPosition).classList += "possible"
                                                }
                                            }
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        } else if (color[startPosition - 4] == white && document.getElementById(startPosition - 4)) {
                                            finalPosition = startPosition - 4
                                            document.getElementById(finalPosition).classList += "possible"
                                        }
                                    }
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                } else if (color[startPosition - 3] == white && document.getElementById(startPosition - 3)) {
                                    finalPosition = startPosition - 3
                                    document.getElementById(finalPosition).classList += "possible"
                                }
                            }
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        } else if (color[startPosition - 2] == white && document.getElementById(startPosition - 2)) {
                            finalPosition = startPosition - 2
                            document.getElementById(finalPosition).classList += "possible"
                        }
                    }
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                } else if (color[startPosition - 1] == white && document.getElementById(startPosition - 1)) {
                    finalPosition = startPosition - 1
                    document.getElementById(finalPosition).classList += "possible"
                }
            }
        } else if (activeType == blackKing) {
            finalPosition = startPosition - 1
            if (color[finalPosition] != black && Math.floor(startPosition / 8) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 1
            if (color[finalPosition] != black && Math.floor(startPosition / 8) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 8
            if (color[finalPosition] != black && Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 8
            if (color[finalPosition] != black && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 7
            if (color[finalPosition] != black && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition + 9
            if (color[finalPosition] != black && Math.floor(startPosition / 8 + 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 7
            if (color[finalPosition] != black && Math.floor(startPosition / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
            finalPosition = startPosition - 9
            if (color[finalPosition] != black && Math.floor(activeFieldID / 8 - 1) == Math.floor(finalPosition / 8) && document.getElementById(finalPosition)) {
                document.getElementById(finalPosition).classList += "possible"
            }
        }
    }
}

//Choose where the function should move to
function toHere(target) {
    //Reset the opponent
    opponent = ""
    //Set the activeType
    var activeType = pieces[activeFieldID]
    //Check if it's white's turn
    if (player == white) {
        finalPosition = target
        //Check if the active piece is a white pawn
        if (activeType == whitePawn) {
            //Check if there is a black piece 
            if (document.getElementById(finalPosition).classList == "piecepossible") {
                //Check if the target field is in the last row
                if (finalPosition < 8 && (deadWhite.includes(whiteRook) || deadWhite.includes(whiteKnight) || deadWhite.includes(whiteBishop) || deadWhite.includes(whiteQueen))) {
                    //Reset variables
                    player = "Niemand"
                    row = 0
                    col = 0
                    indexWhite = 1
                    let reviveType
                    //Modify the deathlists
                    while (row < 8) {
                        col = 0
                        while (col < 2) {
                            //Set a ID
                            id = "black" + indexWhite
                            //Save the type
                            reviveType = document.getElementById(id).innerHTML
                            //Check if the type is a pawn
                            if (reviveType == whitePawn) {
                                //Do nothing
                                document.getElementById(id).innerHTML = whitePawn
                                //Check if the field is empty
                            } else if (reviveType == "") {
                                //Do nothing
                                document.getElementById(id).innerHTML = ""
                            } else {
                                //Make a button with the revive function and the index of the field and the color as parameter and the type as content
                                document.getElementById(id).innerHTML = "<button onclick='revive(" + indexWhite + ", white)'>" + reviveType + "</button>"
                            }
                            col++
                            indexWhite++
                        }
                        row++
                    }
                    //Modify the border of the black Deathlist
                    blackDeathList.style = "border-right: outset 5px #2d2dd4; border-left: outset 5px #4f4ff6; border-bottom: outset 5px #2d2dd4; border-top: outset 5px #4f4ff6"
                    //Set the piece to nothing
                    //Set the color to nothing
                    //Set the occupancy to not occupied
                    //Set the piece of the new position to the active type
                    //Set the occupancy of the new position to occupied
                    //Set the color of the new position to white
                    pieces[activeFieldID] = ""
                    color[activeFieldID] = ""
                    occupancy[activeFieldID] = false
                    pieces[finalPosition] = activeType
                    occupancy[finalPosition] = true
                    color[finalPosition] = white
                } else {
                    opponent = pieces[finalPosition];
                    pieces[activeFieldID] = ""
                    color[activeFieldID] = ""
                    occupancy[activeFieldID] = false
                    pieces[finalPosition] = activeType
                    occupancy[finalPosition] = true
                    color[finalPosition] = white
                    player = black
                }
            }
        } else if (document.getElementById(finalPosition).classList == "piecepossible") {
            opponent = pieces[finalPosition];
            pieces[activeFieldID] = ""
            color[activeFieldID] = ""
            occupancy[activeFieldID] = false
            pieces[finalPosition] = activeType
            occupancy[finalPosition] = true
            color[finalPosition] = white
            player = black
        }
    } else if (player == black) {
        finalPosition = target
        if (activeType == blackPawn) {
            if (document.getElementById(finalPosition).classList == "piecepossible") {
                if (finalPosition > 55 && (deadBlack.includes(blackRook) || deadBlack.includes(blackKnight) || deadBlack.includes(blackBishop) || deadBlack.includes(blackQueen))) {
                    player = "Niemand"
                    row = 0
                    col = 0
                    indexBlack = 1
                    let reviveType

                    while (row < 8) {
                        col = 0
                        while (col < 2) {
                            id = "white" + indexBlack
                            reviveType = document.getElementById(id).innerHTML
                            if (reviveType == blackPawn) {
                                document.getElementById(id).innerHTML = blackPawn
                            } else if (reviveType == "") {
                                document.getElementById(id).innerHTML = ""
                            } else {
                                document.getElementById(id).innerHTML = "<button onclick='revive(" + indexBlack + ", black)'>" + reviveType + "</button>"
                            }
                            indexBlack++
                            col++
                        }
                        row++
                    }
                    whiteDeathList.style = "border-right: outset 5px #2d2dd4; border-left: outset 5px #4f4ff6; border-bottom: outset 5px #2d2dd4; border-top: outset 5px #4f4ff6"

                    pieces[activeFieldID] = ""
                    color[activeFieldID] = ""
                    occupancy[activeFieldID] = false
                    pieces[finalPosition] = activeType
                    occupancy[finalPosition] = true
                    color[finalPosition] = black
                } else {
                    opponent = pieces[finalPosition];
                    pieces[activeFieldID] = ""
                    color[activeFieldID] = ""
                    occupancy[activeFieldID] = false
                    pieces[finalPosition] = activeType
                    occupancy[finalPosition] = true
                    color[finalPosition] = black
                    player = white
                }
            }
        } else if (document.getElementById(finalPosition).classList == "piecepossible") {
            opponent = pieces[finalPosition];
            pieces[activeFieldID] = ""
            color[activeFieldID] = ""
            occupancy[activeFieldID] = false
            pieces[finalPosition] = activeType
            occupancy[finalPosition] = true
            color[finalPosition] = black
            player = white
        }
    }

    if (activeFieldID == finalPosition) {
        opponent = ""
    }

    row = 0
    col = 0
    boardContent = "<table  class='chessboard' id='chessboard'>"

    activeFieldID = 0
    while (row < 8) {
        boardContent += "<tr>"
        while (col < 8) {
            if (pieces[activeFieldID] == "") {
                occupancy[activeFieldID] = false
            }
            else {
                occupancy[activeFieldID] = true
            }
            boardContent += "<td class='piece' onclick='move(" + activeFieldID + ")' id='" + activeFieldID + "'>" + pieces[activeFieldID++] + "</td>"
            col++
        }
        boardContent += "</tr>"
        row++
        col = 0
    }
    boardContent += "</table>"
    chessboard.innerHTML = boardContent

    chessboard.style = "border-right: outset 5px #000000; border-left: outset 5px #2d2d2d; border-bottom: outset 5px #000000; border-top: outset 5px #2d2d2d"

    if (opponent != "") {
        switch (opponent) {
            case blackPawn:
                deadBlackPawns[rowWhite++] = blackPawn
                break;
            case blackRook:
                deadBlack[rowWhite++] = blackRook
                break;
            case blackKnight:
                deadBlack[rowWhite++] = blackKnight
                break;
            case blackBishop:
                deadBlack[rowWhite++] = blackBishop
                break;
            case blackQueen:
                deadBlack[rowWhite++] = blackQueen
                break;
            case blackKing:
                deadBlack[rowWhite++] = blackKing
                break;
            case whitePawn:
                deadWhitePawns[rowBlack++] = whitePawn
                break;
            case whiteRook:
                deadWhite[rowBlack++] = whiteRook
                break;
            case whiteKnight:
                deadWhite[rowBlack++] = whiteKnight
                break;
            case whiteBishop:
                deadWhite[rowBlack++] = whiteBishop
                break;
            case whiteKing:
                deadWhite[rowBlack++] = whiteKing
                break;
            case whiteQueen:
                deadWhite[rowBlack++] = whiteQueen
                break;
        }

        for (let x = 0; x < 16;) {
            x++
            id = "black" + x
            document.getElementById(id).innerHTML = ""
            id = "white" + x
            document.getElementById(id).innerHTML = ""
        }

        indexBlack = 1
        indexWhite = 1

        while (indexBlack <= 16) {
            id = "black" + indexBlack
            switch (deadWhite[indexBlack]) {
                case whiteRook:
                    document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                    break;
                case whiteKnight:
                    document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                    break;
                case whiteBishop:
                    document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                    break;
                case whiteKing:
                    document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                    break;
                case whiteQueen:
                    document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                    break;
                default:
                    document.getElementById(id).innerHTML = ""
                    indexBlack++
            }
        }

        while (indexWhite <= 16) {
            id = "white" + indexWhite
            if (deadBlackPawns[indexWhite] == blackPawn) {
                document.getElementById(id).innerHTML = deadBlackPawns[indexWhite++]
            } else {
                document.getElementById(id).innerHTML = ""
                indexWhite++
            }
        }

        indexBlack = 1
        indexWhite = 1

        while (indexBlack <= 16) {
            id = "black" + indexBlack
            if (deadWhitePawns[indexBlack] == whitePawn) {
                document.getElementById(id).innerHTML = deadWhitePawns[indexBlack++]
            } else {
                indexBlack++
            }
        }

        while (indexWhite <= 16) {
            id = "white" + indexWhite
            switch (deadBlack[indexWhite]) {
                case blackRook:
                    document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                    break;
                case blackKnight:
                    document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                    break;
                case blackBishop:
                    document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                    break;
                case blackQueen:
                    document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                    break;
                case blackKing:
                    document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                    break;
                default:
                    indexWhite++
            }
        }
    }

    if (opponent == blackKing) {
        alert("Weiss hat gewonnen!")
        CreateChessboard()
    }

    if (opponent == whiteKing) {
        alert("Schwarz hat gewonnen!")
        CreateChessboard()
    }

    document.getElementById("player").innerHTML = player + " ist am Zug."
}

function revive(index, colour) {
    if (colour == white) {
        pieces[finalPosition] = deadWhite[index]
        deadWhitePawns[index] = whitePawn
        deadWhite[index] = ""
        occupancy[finalPosition] = true
        color[finalPosition] = white
        player = black
    } else if (colour == black) {
        pieces[finalPosition] = deadBlack[index]
        deadBlackPawns[index] = blackPawn
        deadBlack[index] = ""
        occupancy[finalPosition] = true
        color[finalPosition] = black
        player = white
    }

    blackDeathList.style = "border-right: outset 5px #000000; border-left: outset 5px #2d2d2d; border-bottom: outset 5px #000000; border-top: outset 5px #2d2d2d"
    whiteDeathList.style = "border-right: outset 5px #000000; border-left: outset 5px #2d2d2d; border-bottom: outset 5px #000000; border-top: outset 5px #2d2d2d"

    for (let x = 0; x < 16;) {
        x++
        id = "black" + x
        document.getElementById(id).innerHTML = ""
        id = "white" + x
        document.getElementById(id).innerHTML = ""
    }

    indexBlack = 1
    indexWhite = 1

    while (indexBlack <= 16) {
        id = "black" + indexBlack
        switch (deadWhite[indexBlack]) {
            case whiteRook:
                document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                break;
            case whiteKnight:
                document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                break;
            case whiteBishop:
                document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                break;
            case whiteKing:
                document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                break;
            case whiteQueen:
                document.getElementById(id).innerHTML = deadWhite[indexBlack++]
                break;
            default:
                document.getElementById(id).innerHTML = ""
                indexBlack++
        }
    }

    while (indexWhite <= 16) {
        id = "white" + indexWhite
        if (deadBlackPawns[indexWhite] == blackPawn) {
            document.getElementById(id).innerHTML = deadBlackPawns[indexWhite++]
        } else {
            document.getElementById(id).innerHTML = ""
            indexWhite++
        }
    }

    indexBlack = 1
    indexWhite = 1

    while (indexBlack <= 16) {
        id = "black" + indexBlack
        if (deadWhitePawns[indexBlack] == whitePawn) {
            document.getElementById(id).innerHTML = deadWhitePawns[indexBlack++]
        } else {
            indexBlack++
        }
    }

    while (indexWhite <= 16) {
        id = "white" + indexWhite
        switch (deadBlack[indexWhite]) {
            case blackRook:
                document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                break;
            case blackKnight:
                document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                break;
            case blackBishop:
                document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                break;
            case blackQueen:
                document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                break;
            case blackKing:
                document.getElementById(id).innerHTML = deadBlack[indexWhite++]
                break;
            default:
                indexWhite++
        }
    }

    row = 0
    col = 0
    activeFieldID = 0
    boardContent = "<table  class='chessboard' id='chessboard'>"

    while (row < 8) {
        boardContent += "<tr>"
        while (col < 8) {
            //Set the occupancy
            if (pieces[activeFieldID] == "") {
                occupancy[activeFieldID] = false
            }
            else {
                occupancy[activeFieldID] = true
            }
            //Adding a function to select which piece should move, the fieldID and the piece
            boardContent += "<td class='piece' onclick='move(" + activeFieldID + ")' id='" + activeFieldID + "'>" + pieces[activeFieldID++] + "</td>"
            col++
        }
        boardContent += "</tr>"
        row++
        col = 0
    }
    boardContent += "</table>"
    chessboard.innerHTML = boardContent
    document.getElementById("player").innerHTML = player + " ist am Zug."
}

CreateChessboard()
chessboard.style = "border-right: outset 5px #000000; border-left: outset 5px #2d2d2d; border-bottom: outset 5px #000000; border-top: outset 5px #2d2d2d"
