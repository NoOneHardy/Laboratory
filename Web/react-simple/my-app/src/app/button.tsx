import {useState} from "react";

interface Input {
    value: string
    onClick?: () => void
}

export default function Button({ value, onClick }: Input) {
    
    if (!onClick) onClick = () =>

    return <button className="square" onClick={onClick}>{value}</button>
}