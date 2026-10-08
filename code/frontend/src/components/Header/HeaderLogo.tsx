import Image from "next/image";
import Link from "next/link";

export default function HeaderLogo({ href }: { href: string }) {
  return <Link className="flex-shrink-0" href={href}>
    <Image
      src="/images/logo/PandaStoreLogo2.svg"
      alt="PandaStore"
      width={216}
      height={72}
      className="h-auto w-[180px] shrink-0 object-contain sm:w-[216px]"
    />
  </Link>;
}
