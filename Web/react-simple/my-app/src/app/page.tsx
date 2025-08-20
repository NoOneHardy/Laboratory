import Button from "@/app/button";

export default function Home() {
    const [clicks, setClicks] = useState(0)
    const increment = () => setClicks(clicks + 1)

    return (
        <>
            <Button value={clicks}/>
        </>
    );
}
